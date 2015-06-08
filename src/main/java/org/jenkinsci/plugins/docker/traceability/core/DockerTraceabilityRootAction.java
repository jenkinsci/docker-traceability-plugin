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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.Info;
import hudson.BulkChange;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Action;
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
import java.util.SortedSet;
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
import org.jenkinsci.plugins.docker.traceability.model.DockerTraceabilityReportListener;
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
        final DockerTraceabilityPlugin plugin = DockerTraceabilityPlugin.getInstance();
        return plugin.getConfiguration().isShowRootAction()
                ? "/plugin/docker-traceability/images/24x24/docker.png" : null;
    }

    @Override
    public String getDisplayName() {
        return "Docker Traceability";
    }

    @Override
    public String getUrlName() {
        return "docker-traceability";
    }
       
    /**
     * Submits a new event through Jenkins API.
     * @param inspectData JSON output of docker inspect container (array of container infos)
     * @param hostName Optional name of the host, which submitted the event
     *      &quot;unknown&quot; by default
     * @param hostId Optional host ID. 
     *      &quot;unknown&quot; by default
     * @param status Optional status of the container. 
     *      By default, an artificial {@link DockerEventType#NONE} will be used.    
     * @param time Optional time when the event happened. 
     *      The time is specified in seconds since January 1, 1970, 00:00:00 GMT
     *      Default value - current time
     * @param environment Optional field, which describes the environment
     * @param imageName Optional field, which provides the name of the image
     * @return {@link HttpResponse}
     * @throws IOException Request processing error
     * @throws ServletException Servlet error
     */
    //TODO: parameters check
    @RequirePOST
    public HttpResponse doSubmitContainerStatus(
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
        final long eventTime = time != 0 ? time : System.currentTimeMillis()/1000;
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
                    /* InspectImageResponse */ null, new LinkedList<String>(), effectiveEnvironment);
            DockerTraceabilityReportListener.fire(res);
        }
        return HttpResponses.ok();
    } 
       
    /**
     * Submits a new {@link DockerTraceabilityReport} via API.
     * @param json String representation of {@link DockerTraceabilityReport}
     * @return {@link HttpResponse}
     * @throws ServletException Servlet error
     * @throws IOException Processing error
     */
    @RequirePOST
    public HttpResponse doSubmitReport(@QueryParameter(required = true) String json) 
            throws IOException, ServletException { 
        checkPermission(DockerTraceabilityPlugin.SUBMIT);
        ObjectMapper mapper = new ObjectMapper();
        final DockerTraceabilityReport report = mapper.readValue(json, DockerTraceabilityReport.class);
        DockerTraceabilityReportListener.fire(report);
        return HttpResponses.ok();
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
     * @param id Container ID. Method supports full 64-char IDs only.
     * @throws IOException Cannot save the updated {@link DockerTraceabilityRootAction}
     * @throws ServletException Servlet exception
     */
    @RequirePOST
    public HttpResponse doDeleteContainer(@QueryParameter(required = true) String id) 
            throws IOException, ServletException {  
        checkPermission(DockerTraceabilityPlugin.DELETE);
        removeContainerID(id);
        return HttpResponses.ok();
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
     * Supports filers. Missing &quot;since&quot; and &quot;until&quot; 
     * @param id ID of the container, for which the info should be retrieved.
     *    Short container IDs are not supported.
     * @param format Format used in the response
     * @throws IOException Processing error
     * @throws ServletException Servlet error
     * @return Response available in different formats, including a JSON output compatible with docker inspect
     */
    public HttpResponse doRawContainerInfo(@QueryParameter(required = true) String id, @QueryParameter(required = false) final String format)
            throws IOException, ServletException {
        checkPermission(DockerTraceabilityPlugin.READ_DETAILS);
        
        //TODO: check containerID format
        final DockerTraceabilityReport report = DockerTraceabilityHelper.getLastReport(id);
        if (report == null) {
            return HttpResponses.error(404, "No info available for the containerId=" + id);
        }
        final InspectContainerResponse inspectInfo = report.getContainer();
        if (inspectInfo == null) {
            assert false : "Input logic should reject such cases";
            return HttpResponses.error(500, "Cannot retrieve the container's status"); 
        }
        
        InspectContainerResponse[] out = {inspectInfo};
        return toFormattedResponse(out, format);
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
     * @param format Format used in the response.
     * @throws IOException Processing error
     * @throws ServletException Servlet error
     * @return Response available in different formats. may be an error if something breaks.
     */
    public HttpResponse doQueryContainer( 
            @QueryParameter(required = true) String id,
            @QueryParameter(required = false) String mode,
            @QueryParameter(required = false) long since,
            @QueryParameter(required = false) long until,
            @QueryParameter(required = false) String format)
            throws IOException, ServletException {     
        checkPermission(DockerTraceabilityPlugin.READ_DETAILS);
        
        final QueryMode queryMode = QueryMode.fromString(mode);
        final long maxTime = (until != 0) ? until : Long.MAX_VALUE;
        final long minTime = (since != 0) ? since : Long.MIN_VALUE;
        
        DockerDeploymentFacet facet = DockerDeploymentFacet.getDeploymentFacet(id);
        if (facet == null) {
            return HttpResponses.error(404, "No info available for the containerId=" + id);
        }
        
        final SortedSet<DockerContainerRecord> deploymentRecords = facet.getDeploymentRecords();
        List<Object> result = new ArrayList<Object>(deploymentRecords.size());
        for (DockerContainerRecord record : deploymentRecords) {
            // time filters
            final long eventTime = record.getReport().getEvent().getTime();
            if (eventTime < minTime || eventTime > maxTime) {
                continue;
            }
            
            // Report data
            final DockerTraceabilityReport report = record.getReport();
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
        
        return toFormattedResponse(result, format);
    }  
    
    /**
     * Retrieves the latest raw status via API.
     *
     * @param id ID of the image, for which the info should be retrieved.
     *    Short container IDs are not supported.
     * @param format Format used in the output response.
     *
     * @throws IOException Processing error
     * @throws ServletException Servlet error
     *
     * @return Response ({@link HttpResponse}) available in different formats
     */
    public HttpResponse doRawImageInfo(@QueryParameter(required = true) String id, @QueryParameter(required = false) String format)
            throws IOException, ServletException {
        checkPermission(DockerTraceabilityPlugin.READ_DETAILS);
        
        final InspectImageResponse report = DockerTraceabilityHelper.getLastInspectImageResponse(id);
        if (report == null) {   
            return HttpResponses.error(404, "No info available for the imageId=" + id);
        }
        
        InspectImageResponse[] out = {report};
        return toFormattedResponse(out, format);
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
     * Represents the different response formats (JSON, pretty JSON, XML).
     */
    private enum ResponseFormat {
        JSON("application/json;charset=UTF-8", false),
        PRETTYJSON("application/json;charset=UTF-8", true),
        XML("text/xml;charset=UTF-8", false);

        private String contentType;
        
        private boolean pretty;

        private static final ResponseFormat DEFAULT = JSON;

        private ResponseFormat(final String contentType, final boolean pretty) {
            this.contentType = contentType;
            this.pretty = pretty;
        }

        public static ResponseFormat fromAlias(final String alias) {
            if (alias == null) {
                return DEFAULT;
            }
            if (alias.equals("json")) {
                return JSON;
            } else if (alias.equals("json-pretty")) {
                return PRETTYJSON;
            } else if (alias.equals("xml")) {
                 // Related to https://issues.jenkins-ci.org/browse/JENKINS-28727
                throw new IllegalStateException("Unsupported format: " + alias);
            } else {
                throw new IllegalStateException("Unsupported format: " + alias);
            }
        }

        public String getContentType() {
            return contentType;
        }

        public Boolean getPretty() {
            return pretty;
        }
    }

    /**
     * Serves the response and manages its output format in the response.
     *
     * @param item Data to be serialized
     * @param format Format used in the response
     *
     * @return HTTP response with MIME type
     */
    private static HttpResponse toFormattedResponse(final Object item, final String format) {
        return new HttpResponse() {
            @Override
            public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
                ObjectMapper mapper = new ObjectMapper();
                ResponseFormat responseFormat = ResponseFormat.fromAlias(format);
                rsp.setContentType(responseFormat.getContentType());
                if (responseFormat.getPretty()) {
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                }
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
    
    /**
     * Gets the {@link DockerTraceabilityRootAction} of Jenkins instance.
     * @return Instance or null if it is not available
     */
    public static @CheckForNull DockerTraceabilityRootAction getInstance() {
        Jenkins j = Jenkins.getInstance();
        if (j == null) {
            return null;
        }
        
        @CheckForNull DockerTraceabilityRootAction action = null;
        for (Action rootAction : j.getActions()) {
            if (rootAction instanceof DockerTraceabilityRootAction) {
                action = (DockerTraceabilityRootAction) rootAction;
                break;
            }
        } 
        return action;
    }
}
