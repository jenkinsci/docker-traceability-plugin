/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.docker.traceability.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.Info;
import hudson.BulkChange;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Api;
import hudson.model.Fingerprint;
import hudson.model.RootAction;
import hudson.model.Run;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.search.Search;
import hudson.search.SearchIndex;
import hudson.search.SearchIndexBuilder;
import hudson.search.SearchableModelObject;
import hudson.security.Permission;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import static jenkins.model.Jenkins.XSTREAM;
import org.acegisecurity.AccessDeniedException;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.docker.commons.fingerprint.DockerFingerprints;
import org.jenkinsci.plugins.docker.traceability.DockerEventListener;
import org.jenkinsci.plugins.docker.traceability.DockerTraceabilityPlugin;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerContainerRecord;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerDeploymentFacet;
import org.jenkinsci.plugins.docker.traceability.model.DockerAPIReport;
import org.jenkinsci.plugins.docker.traceability.model.DockerEvent;
import org.jenkinsci.plugins.docker.traceability.model.DockerEventType;
import org.jenkinsci.plugins.docker.traceability.model.DockerInfo;
import org.jenkinsci.plugins.docker.traceability.model.DockerTraceabilityReport;
import org.jenkinsci.plugins.docker.traceability.util.FingerprintsHelper;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Provides a button, which allows to trace fingerprints.
 * @author Oleg Nenashev
 */
@Extension
@ExportedBean
public class DockerTraceabilityRootAction implements RootAction, SearchableModelObject, Saveable {

    private final static Logger LOGGER = Logger.getLogger(DockerTraceabilityPlugin.class.getName());
    
    private @CheckForNull Set<String> containerIDs;

    public DockerTraceabilityRootAction() {
        load();
    }
    
    public Api getApi() {
        return new Api(this);
    }

    /**
     * Get a list of all Docker container IDs.
     * @return Docker container IDs.
     */
    public synchronized @Nonnull Set<String> getContainerIDs() {
        return (containerIDs == null) ?  Collections.<String>emptySet() : new TreeSet<String>(containerIDs);
    }
    
    @Exported
    public synchronized @Nonnull List<DockerAPIReport> records() {
        if (containerIDs == null) {
            return Collections.emptyList();
        }
        
        final List<DockerAPIReport> res = new ArrayList<DockerAPIReport>(containerIDs.size());
        for (String containerId : containerIDs) {
            DockerAPIReport apiReport = DockerAPIReport.forContainer(containerId);
            if (apiReport != null) {
                res.add(apiReport);
            }
        }
        return res;
    }
    
    /**
     * Adds new container ID to the registry.
     * If the value already exists, it will be ignored.
     * @param containerID Container ID.
     * @throws IOException Cannot save the list to the disk
     */
    public synchronized @Nonnull void addContainerID(@Nonnull String containerID) 
            throws IOException {
        if (containerIDs == null) {
            containerIDs = new HashSet<String>();
        }
        if (!containerIDs.contains(containerID)) {
            containerIDs.add(containerID);
            save();
        }
    }
    
    /**
     * Removes the container ID from the registry.
     * @param containerID Container ID.
     * @throws IOException Cannot save the list to the disk
     */
    public synchronized @Nonnull void removeContainerID(@Nonnull String containerID) 
            throws IOException {
        if (containerIDs != null && containerIDs.contains(containerID)) {
            containerIDs.remove(containerID);
            save();
        }
    }

    @Restricted(NoExternalUse.class)
    public @CheckForNull Fingerprint getFingerprint(@Nonnull String containerId) {
        return DockerTraceabilityHelper.of(containerId);
    }
    
    public @CheckForNull DockerDeploymentFacet getDeploymentFacet(@Nonnull String containerId) {
        Fingerprint fp = DockerTraceabilityHelper.of(containerId);
        return (fp != null) ? FingerprintsHelper.getFacet(fp, DockerDeploymentFacet.class) : null;        
    } 
    
    public @CheckForNull DockerContainerRecord getLastDeploymentRecord(@Nonnull String containerId) {
        DockerDeploymentFacet facet = getDeploymentFacet(containerId);
        return (facet != null) ? facet.getLatest() : null;
    } 
    
    /**
     * Gets a last container record for every registered container.
     * @return List of container records for all entries.
     */
    public synchronized @Nonnull List<DockerContainerRecord> getContainerRecords() {
        if (containerIDs == null) {
            return Collections.emptyList();
        }
        
        final List<DockerContainerRecord> res = new ArrayList<DockerContainerRecord>(containerIDs.size());
        for (String containerId : containerIDs) {
            DockerContainerRecord rec = DockerTraceabilityHelper.getLastContainerRecord(containerId);
            if (rec != null) {
                res.add(rec);
            }
        }
        return res;
    }
    
    @Override
    public String getIconFileName() {
        return "/plugin/docker-traceability/images/24x24/docker.png";
    }

    @Override
    public String getDisplayName() {
        return "Docker Traceability";
    }

    @Override
    public String getUrlName() {
        return "docker-traceability";
    }
    
    

    //TODO: remove
    /**
     * 
     * @param req Incoming request
     * @param rsp Response
     * @param imageId image id
     * @param jobName job name, to which the facet should be attached
     * @throws IOException Request processing error
     * @throws ServletException Servlet error
     * @deprecated Test only 
     */
    @Deprecated
    public void doTestSubmitBuildRef(StaplerRequest req, StaplerResponse rsp,
            @QueryParameter(required = true) String imageId,
            @QueryParameter(required = true) String jobName) throws IOException, ServletException {
        final Jenkins j = Jenkins.getInstance();
        if (j == null) {
            throw new IOException("Jenkins instance is not active");
        }
        final AbstractProject item = j.getItem(jobName, j, AbstractProject.class);
        final Run latest = item != null ? item.getLastBuild() : null;
        if (latest == null) {
            throw new IOException("Cannot find a project or run to modify"); 
        }
        
        DockerFingerprints.addFromFacet(null,imageId, latest);
        rsp.sendRedirect2(j.getRootUrl());
    }
       
    /**
     * Submits a new event through Jenkins API.
     * @param req Incoming request
     * @param rsp Output response
     * @param inspectData JSON output of docker inspect container (array of container infos)
     * @param hostName Optional name of the host, which submitted the event
     *      &quot;unknown&quot; by default
     * @param hostId Optional host ID. 
     *      &quot;unknown&quot; by default
     * @param status Optional status of the container. 
     *      By default, an artificial {@link DockerEventType#NONE} will be used.    
     * @param time Optional time when the event happened. 
     *      The time is specified in milliseconds since January 1, 1970, 00:00:00 GMT
     *      Default value - current time
     * @param environment Optional field, which describes the environment
     * @param imageName Optional field, which provides the name of the image
     * @throws IOException Request processing error
     * @throws ServletException Servlet error
     */
    //TODO: parameters check
    @RequirePOST
    public void doSubmitContainerStatus(StaplerRequest req, StaplerResponse rsp,
            @QueryParameter(required = true) String inspectData,
            @QueryParameter(required = false) String hostId,
            @QueryParameter(required = false) String hostName,
            @QueryParameter(required = false) String status,
            @QueryParameter(required = false) long time,
            @QueryParameter(required = false) @CheckForNull String environment,
            @QueryParameter(required = false) @CheckForNull String imageName
    ) throws IOException, ServletException { 
        checkPermission(DockerTraceabilityPlugin.SUBMIT);
        final ObjectMapper mapper = new ObjectMapper();
        final InspectContainerResponse[] inspectContainerResponses = mapper.readValue(inspectData, InspectContainerResponse[].class);
        final long eventTime = time != 0 ? time : new Date().getTime();
        final String effectiveHostName = StringUtils.isNotBlank(hostName) ? hostName : "unknown";
        final String effectiveHostId = StringUtils.isNotBlank(hostId) ? hostId : "unknown";
        final String effectiveStatus = StringUtils.isNotBlank(status) 
                ? status.toUpperCase(Locale.ENGLISH) : DockerEventType.NONE.toString();
        final String effectiveImageName = hudson.Util.fixEmpty(imageName);
        final String effectiveEnvironment = hudson.Util.fixEmpty(environment);
        
        
        for (InspectContainerResponse inspectContainerResponse : inspectContainerResponses) {
            final Event event = new DockerEvent(effectiveStatus, inspectContainerResponse.getImageId(), 
                    effectiveHostId, eventTime).toDockerEvent();
            final Info hostInfo = new DockerInfo(effectiveHostId, effectiveHostName).toInfo();

            DockerTraceabilityReport res = new DockerTraceabilityReport(event, hostInfo,
                    inspectContainerResponse, 
                    inspectContainerResponse.getImageId(), effectiveImageName,
                    new LinkedList<String>(), effectiveEnvironment);
            DockerEventListener.fire(res);
        }
    } 
       
    /**
     * Submits a new {@link DockerTraceabilityReport} via API.
     * @param req Incoming request
     * @param rsp Output response
     * @param json String representation of {@link DockerTraceabilityReport}
     * @throws ServletException Servlet error
     * @throws IOException Processing error
     */
    @RequirePOST
    public void doSubmitEvent(StaplerRequest req, StaplerResponse rsp, 
            @QueryParameter(required = true) String json) 
            throws IOException, ServletException { 
        checkPermission(DockerTraceabilityPlugin.SUBMIT);
        ObjectMapper mapper = new ObjectMapper();
        final DockerTraceabilityReport report = mapper.readValue(json, DockerTraceabilityReport.class);
        DockerEventListener.fire(report);
    }
    
    /**
     * Gets a container {@link Fingerprint} page.
     * @param req Stapler request
     * @param rsp Stapler response
     * @param id Container ID. Method supports full 64-char IDs only.
     * @throws IOException Request processing error
     * @throws ServletException Servlet error
     */
    public void doContainer(StaplerRequest req, StaplerResponse rsp, 
            @QueryParameter(required = true) String id) 
            throws IOException, ServletException {  
        checkPermission(Jenkins.READ);
        Jenkins j = Jenkins.getInstance();
        if (j == null) {
            rsp.sendError(500, "Jenkins is not ready");
            return;
        }
        
        String fingerPrintHash = DockerTraceabilityHelper.getContainerHash(id);
        rsp.sendRedirect2(j.getRootUrl()+"fingerprint/"+fingerPrintHash);
    }
    
    /**
     * Removes the container reference from the registry.
     * @param req Stapler request
     * @param rsp Stapler response
     * @param id Container ID. Method supports full 64-char IDs only.
     * @throws IOException Cannot save the updated {@link DockerTraceabilityRootAction}
     * @throws ServletException Servlet exception
     */
    @RequirePOST
    public void doRemoveContainer(StaplerRequest req, StaplerResponse rsp, 
            @QueryParameter(required = true) String id) 
            throws IOException, ServletException {  
        checkPermission(DockerTraceabilityPlugin.DELETE);
        removeContainerID(id);
    }
    
    /**
     * Gets an image {@link Fingerprint} page.
     * @param req Stapler request
     * @param rsp Stapler response
     * @param id Image ID. Method supports full 64-char IDs only.
     * @throws IOException  Request processing error
     * @throws ServletException Servlet error
     */
    public void doImage(StaplerRequest req, StaplerResponse rsp, 
            @QueryParameter(required = true) String id) 
            throws IOException, ServletException {  
        checkPermission(Jenkins.READ);
        Jenkins j = Jenkins.getInstance();
        if (j == null) {
            rsp.sendError(500, "Jenkins is not ready");
            return;
        }
        
        String fingerPrintHash = DockerTraceabilityHelper.getImageHash(id);
        rsp.sendRedirect2(j.getRootUrl()+"fingerprint/"+fingerPrintHash);
    }
    
    //TODO: filtering by container ID, imageID, containerName, imageName, hostName, hostID, environment
    /**
     * Retrieves the latest container status via API.
     * The output will be retrieved in JSON. Supports filers. Missing 
     * &quot;since&quot; and &quot;until&quot; 
     * @param req Incoming request
     * @param rsp Output response
     * @param containerId ID of the container, for which the info should be retrieved.
     *    Short container IDs are not supported.
     * @throws IOException Processing error
     * @throws ServletException Servlet error
     */
    public void doRawContainerInfo(StaplerRequest req, StaplerResponse rsp, 
            @QueryParameter(required = true) String containerId) 
            throws IOException, ServletException {     
        checkPermission(DockerTraceabilityPlugin.READ_DETAILS);
        
        //TODO: check containerID format
        final DockerTraceabilityReport report = DockerTraceabilityHelper.getLastReport(containerId);
        if (report == null) {
            rsp.sendError(404, "No info available for the containerId=" + containerId);
            return;
        }
        final InspectContainerResponse inspectInfo = report.getContainer();
        if (inspectInfo == null) {
            assert false : "Input logic should reject such cases";
            rsp.sendError(500, "Cannot retrieve the container's status");
        }
        
        // Return raw JSON in the response
        ObjectMapper mapper = new ObjectMapper();
        InspectContainerResponse[] out = {inspectInfo};
        mapper.writeValue(rsp.getOutputStream(), out);
    }  
    
    //TODO: More filtering
    /**
     * Queries container statuses via API.
     * The output will be retrieved in JSON. Supports filters.
     * @param id ID of the container, for which the info should be retrieved.
     *    Short container IDs are not supported.
     * @param mode {@link QueryMode}. Default value - {@link QueryMode#inspectContainer}
     * @param since Optional starting time. 
     *      If the value equals to 0, the filter will be ignored (default in {@link QueryParameter}).
     * @param until End time. 
     *      If the value equals to 0, the filter will be ignored (default in {@link QueryParameter}).
     * @throws IOException Processing error
     * @throws ServletException Servlet error
     * @return Response containing the output JSON. may be an error if something breaks.
     */
    public HttpResponse doQueryContainer( 
            @QueryParameter(required = true) String id,
            @QueryParameter(required = false) String mode,
            @QueryParameter(required = false) long since,
            @QueryParameter(required = false) long until) 
            throws IOException, ServletException {     
        checkPermission(DockerTraceabilityPlugin.READ_DETAILS);
        
        final QueryMode queryMode = QueryMode.fromString(mode);
        final long maxTime = (until != 0) ? until : Long.MAX_VALUE;
        final long minTime = (since != 0) ? since : Long.MIN_VALUE;
        
        DockerDeploymentFacet facet = DockerDeploymentFacet.getDeploymentFacet(id);
        if (facet == null) {
            return HttpResponses.error(404, "No info available for the containerId=" + id);
        }
        
        List<DockerContainerRecord> deploymentRecords = facet.getDeploymentRecords();
        List<Object> result = new ArrayList<Object>(deploymentRecords.size());
        for (DockerContainerRecord record : deploymentRecords) {
            // time filters
            final long eventTime = record.getEvent().getEvent().getTime();
            if (eventTime < minTime || eventTime > maxTime) {
                continue;
            }
            
            // Report data
            final DockerTraceabilityReport report = record.getEvent();
            switch (queryMode) {
                case all:
                    result.add(report);
                    break;
                case events:
                    result.add(report.getEvent());
                    break;
                case inspectContainer:
                    InspectContainerResponse containerResponse = report.getContainer();
                    if (containerResponse != null) {
                        result.add(containerResponse);
                    }
                    break;
                case inspectImage:
                    InspectImageResponse imageResponse = report.getImage();
                    if (imageResponse != null) {
                        result.add(imageResponse);
                    }
                    break;   
                case hostInfo:
                    result.add(report.getHostInfo());
                    break;    
                default:
                    throw new IllegalStateException("Unsupported query mode: "+queryMode);
            }
        }
        
        // Return raw JSON in the response
        return toJSONResponse(result);
    }  
    
    /**
     * Retrieves the latest raw status via API.
     * The output will be retrieved in JSON.
     * @param req Incoming request
     * @param rsp Output response
     * @param id ID of the image, for which the info should be retrieved.
     *    Short container IDs are not supported.
     * @throws IOException Processing error
     * @throws ServletException Servlet error
     */
    public void doRawImageInfo(StaplerRequest req, StaplerResponse rsp, 
            @QueryParameter(required = true) String id) 
            throws IOException, ServletException {     
        checkPermission(DockerTraceabilityPlugin.READ_DETAILS);
        
        final InspectImageResponse report = DockerTraceabilityHelper.getLastInspectImageResponse(id);
        if (report == null) {
            rsp.sendError(404, "No info available for the imageId=" + id);
            return;
        }
        
        // Return raw JSON in the response
        ObjectMapper mapper = new ObjectMapper();
        InspectImageResponse[] out = {report};
        mapper.writeValue(rsp.getOutputStream(), out);
    } 
    
    /**
     * Check permission.
     * Also prohibits the access if Jenkins has not been started yet.
     * @param p Permission to be checked
     * @throws AccessDeniedException Access denied
     */
    private void checkPermission(Permission p) throws AccessDeniedException {
        final Jenkins j = Jenkins.getInstance();
        if (j == null) {
            throw new AccessDeniedException("Cannot retrieve Jenkins instance. "
                    + "Probably, the service is starting or shutting down");
        }     
        j.checkPermission(p);
    }
    
    /**
     * Check permission.
     * @param p Permission to be checked
     * @retun false if the user has no permission or if Jenkins is unavailable
     */
    private boolean hasPermission(Permission p) {
        final Jenkins j = Jenkins.getInstance();
        return (j == null) ? false : j.hasPermission(p);
    }
    
    @Restricted(NoExternalUse.class)
    public Permission getRequiredPermission() {
        return Jenkins.READ;
    }

    @Override
    public Search getSearch() {
        return new Search();
    }

    @Override
    public String getSearchName() {
        return getDisplayName();
    }

    @Override
    public String getSearchUrl() {
        return getUrlName();
    }

    @Override
    public SearchIndex getSearchIndex() {
        return makeSearchIndex().make();
    }
    
    private synchronized SearchIndexBuilder makeSearchIndex() {
        final SearchIndexBuilder searchIndexBuilder = new SearchIndexBuilder();
        Jenkins j = Jenkins.getInstance();
        if (j == null || containerIDs == null) {
            return searchIndexBuilder; // cannot construct URLs 
        }
        
        // Add container IDs and hashes for each registered container id.
        for (String containerID : containerIDs) {
            String containerHash = DockerTraceabilityHelper.getContainerHash(containerID);
            searchIndexBuilder.add("container?id="+containerID, containerID, containerHash);
        }
        return searchIndexBuilder;
    }

    private @Nonnull XmlFile getConfigFile() throws IOException {
        final Jenkins j = Jenkins.getInstance();
        if (j==null) {
            throw new IOException("Jenkins instance is not ready, cannot retrieve the root directory");
        }
        
        return new XmlFile(XSTREAM, new File(j.getRootDir(), DockerTraceabilityRootAction.class.getName()+".xml"));
    }
    
    @Override
    public void save() throws IOException {
        if(BulkChange.contains(this))   return;
        getConfigFile().write(this);
        SaveableListener.fireOnChange(this, getConfigFile());
    }
    
    /**
     * Loads the other data from disk if it's available.
     */
    private synchronized void load() {
        if (containerIDs != null) {
            containerIDs.clear();
        }

        XmlFile config = null; 
        try {
            config = getConfigFile();
            if(config.exists())
                config.unmarshal(this);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load the configuration from config path = "+config,e);
        }
    }
    
    /**
     * Serves the JSON response.
     * @param item Data to be serialized to JSON
     * @return HTTP response with application/json MIME type
     */
    private static HttpResponse toJSONResponse(final Object item) {
        return new HttpResponse() {
            @Override
            public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
                ObjectMapper mapper = new ObjectMapper(); 
                rsp.setContentType("application/json;charset=UTF-8");
                mapper.writeValue(rsp.getWriter(), item);
            }
        };
    }
    
    private enum QueryMode {
        inspectContainer,
        inspectImage,
        events,
        hostInfo,
        all;
        
        private static final QueryMode DEFAULT = inspectContainer;
        
        public static QueryMode fromString(@CheckForNull String str) {
            if (str == null) {
                return DEFAULT;
            }
            
            try {
                return valueOf(str);
            } catch (IllegalArgumentException ex) {
                return DEFAULT;
            }
        }
    }
}
