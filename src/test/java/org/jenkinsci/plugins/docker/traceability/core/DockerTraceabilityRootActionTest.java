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
import com.gargoylesoftware.htmlunit.Page;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import hudson.model.Action;
import hudson.model.Fingerprint;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.docker.commons.fingerprint.DockerFingerprints;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerContainerRecord;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerDeploymentFacet;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerDeploymentRefFacet;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerInspectImageFacet;
import org.jenkinsci.plugins.docker.traceability.model.DockerTraceabilityReport;
import org.jenkinsci.plugins.docker.traceability.samples.JSONSamples;
import org.jenkinsci.plugins.docker.traceability.test.FingerprintTestUtil;
import org.jenkinsci.plugins.docker.traceability.util.FingerprintsHelper;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;

/**
 * Tests for {@link DockerTraceabilityRootAction}.
 * @author Oleg Nenashev
 */
public class DockerTraceabilityRootActionTest {
    
    @Rule
    public JenkinsRule j = new JenkinsRule();
    
    @Mock private StaplerRequest req;
    @Mock private StaplerResponse rsp;
    
    @Test
    public void rawContainerDockerInspectSubmissions() throws Exception {
        
        // Read data from resources
        String inspectData = JSONSamples.inspectContainerData.readString();
        InspectContainerResponse inspectResponse = JSONSamples.inspectContainerData.
                readObject(InspectContainerResponse[].class)[0];
        final String containerId = inspectResponse.getId();
        final String imageId = inspectResponse.getImageId();
        
        // Init system data
        JenkinsRule.WebClient client = j.createWebClient();
        final DockerTraceabilityRootAction action = DockerTraceabilityHelper.getTraceabilityAction();
        assertNotNull(action);
             
        // Prepare a run with Fingerprints and referenced facets
        createTestBuildRefFacet(imageId, "test");
        
        // Submit JSON
        action.doSubmitContainerStatus(inspectData, null, null, null, 0, null, null);
        
        // Ensure there's a fingerprint for container, which refers the image
        final DockerDeploymentFacet containerFacet = assertExistsDeploymentFacet(containerId, imageId);
        
        // Ensure there's a fingerprint for image
        final DockerDeploymentRefFacet containerRefFacet = assertExistsDeploymentRefFacet(containerId, imageId);
        
        // Try to call the actions method to retrieve the data
        final Page res;
        try {
            res = client.goTo("docker-traceability/rawContainerInfo?id="+containerId, null);
        } catch (Exception ex) {
            ex.getMessage();
            throw new AssertionError("Cannot get a response from rawInfo page", ex);
        }
        final String responseJSON = res.getWebResponse().getContentAsString();
        ObjectMapper mapper= new ObjectMapper();
        final InspectContainerResponse[] parsedData = mapper.readValue(responseJSON, InspectContainerResponse[].class);
        assertEquals(1, parsedData.length);       
    }
    
    /**
     * Checks {@link DockerEventsAction#doSubmitEvent(org.kohsuke.stapler.StaplerRequest, 
     * org.kohsuke.stapler.StaplerResponse, java.lang.String) }
     * @throws Exception test failure
     */
    @Test
    public void submitEvent() throws Exception {
        // Read data from resources
        String reportString = JSONSamples.submitReport.readString();
        DockerTraceabilityReport report = JSONSamples.submitReport.
                readObject(DockerTraceabilityReport.class);
        final String containerId = report.getContainer().getId();
        final String imageId = report.getImageId();
        
        // Init system data
        // TODO: replace by a helper method from the branch
        JenkinsRule.WebClient client = j.createWebClient();
        @CheckForNull DockerTraceabilityRootAction action = null;
        for (Action rootAction : j.getInstance().getActions()) {
            if (rootAction instanceof DockerTraceabilityRootAction) {
                action = (DockerTraceabilityRootAction) rootAction;
                break;
            }
        }    
        assertNotNull(action);
        
        // Prepare a run with Fingerprints and referenced facets
        createTestBuildRefFacet(imageId, "test");
        
        // Submit JSON
        action.doSubmitReport(reportString);
        
        // Ensure there's are expected fingerprints
        final DockerDeploymentFacet containerFacet = assertExistsDeploymentFacet(containerId, imageId);
        final DockerDeploymentRefFacet containerRefFacet = assertExistsDeploymentRefFacet(containerId, imageId);
        final DockerInspectImageFacet inspectImageFacet = assertExistsInspectImageFacet(imageId);
        
        // Try to call the actions method to retrieve the data
        final Page res;
        try {
            res = client.goTo("docker-traceability/rawImageInfo?id="+imageId, null);
        } catch (Exception ex) {
            ex.getMessage();
            throw new AssertionError("Cannot get a response from rawInfo page", ex);
        }
        final String responseJSON = res.getWebResponse().getContentAsString();
        ObjectMapper mapper= new ObjectMapper();
        final InspectImageResponse[] parsedData = mapper.readValue(responseJSON, InspectImageResponse[].class);
        assertEquals(1, parsedData.length); 
        InspectImageResponse apiResponse = parsedData[0];
        assertEquals(imageId, apiResponse.getId()); 
    }
    
    @Test
    public void containerIDs_CRUD() throws Exception {
        // TODO: replace by a helper method from the branch
        JenkinsRule.WebClient client = j.createWebClient();
        @CheckForNull DockerTraceabilityRootAction action = null;
        for (Action rootAction : j.getInstance().getActions()) {
            if (rootAction instanceof DockerTraceabilityRootAction) {
                action = (DockerTraceabilityRootAction) rootAction;
                break;
            }
        }    
        assertNotNull(action);
        
        final String id1 = generateContainerId("1");
        final String id2 = generateContainerId("2");
        final String id3 = generateContainerId("3");
        
        // Check consistency of create/update commands
        action.addContainerID(id1);
        assertEquals(1, action.getContainerIDs().size());
        action.addContainerID(id2);
        assertEquals(2, action.getContainerIDs().size());
        action.addContainerID(id2);
        assertEquals(2, action.getContainerIDs().size());
        
        // Remove data using API. First entry is non-existent
        action.doDeleteContainer(id3);
        assertEquals(2, action.getContainerIDs().size());
        action.doDeleteContainer(id1);
        assertEquals(1, action.getContainerIDs().size());
        action.doDeleteContainer(id1);
        assertEquals(1, action.getContainerIDs().size());
        
        // Reload the data and ensure the status has been persisted correctly
        action = new DockerTraceabilityRootAction();
        assertEquals(1, action.getContainerIDs().size());
        for (String id : action.getContainerIDs()) {
            assertEquals(id2, id);
        }
    }
    
    /**
     * Prepare a run with Fingerprints and referenced facets.
     * @param imageId image Id to refer
     * @param jobName job name
     * @return Run marked by the facet
     */
    private FreeStyleBuild createTestBuildRefFacet(String imageId, String jobName) throws Exception {
        FreeStyleProject prj = j.jenkins.getItemByFullName(jobName, FreeStyleProject.class);
        if ( prj == null) {
            prj = j.createFreeStyleProject(jobName);  
        }
        FreeStyleBuild bld = j.buildAndAssertSuccess(prj);
        FingerprintTestUtil.injectFromFacet(bld, imageId);
        return bld;
    }
    
    /**
     * Checks that the existence {@link DockerDeploymentFacet}.
     * @param containerId Container ID
     * @param imageId image ID
     * @return Facet (if validation passes)
     * @throws IOException test failure
     * @throws AssertionError Validation failure
     */
    private @Nonnull DockerDeploymentFacet assertExistsDeploymentFacet
            (@Nonnull String containerId, @Nonnull String imageId) throws IOException {
        final Fingerprint containerFP = DockerTraceabilityHelper.ofValidated(containerId);
        assertNotNull(containerFP);
        final DockerDeploymentFacet containerFacet = 
                FingerprintsHelper.getFacet(containerFP, DockerDeploymentFacet.class);
           
        assertNotNull(containerFacet);
        assertEquals(1, containerFacet.getDeploymentRecords().size());
        final DockerContainerRecord record = containerFacet.getLatest();           
        assertNotNull(containerFacet.getLatest());
        assertEquals(containerId, record.getContainerId());
        assertEquals(DockerTraceabilityHelper.getImageHash(imageId), record.getImageFingerprintHash());
        return containerFacet;
    }
        
    /**
     * Checks that the existence {@link DockerDeploymentRefFacet}.
     * @param containerId Container ID
     * @param imageId image ID
     * @return Facet (if validation passes)
     * @throws IOException test failure
     * @throws AssertionError Validation failure
     */        
    public DockerDeploymentRefFacet assertExistsDeploymentRefFacet
        (@Nonnull String containerId, @Nonnull String imageId) throws IOException {
        final Fingerprint imageFP = DockerFingerprints.of(imageId);
        assertNotNull(imageFP);
        final DockerDeploymentRefFacet containerRefFacet = 
                FingerprintsHelper.getFacet(imageFP, DockerDeploymentRefFacet.class);
        
        assertNotNull(containerRefFacet);
        assertEquals(1, containerRefFacet.getContainerIds().size());
        for (String containerRefId : containerRefFacet.getContainerIds()) {
            assertNotNull(containerRefFacet.getLastStatus(containerRefId));
        }
        assertEquals(containerId, containerRefFacet.getContainerIds().toArray()[0]);
        return containerRefFacet;
    }
        
    /**
     * Checks that the existence {@link DockerInspectImageFacet}.
     * @param imageId image ID
     * @return Facet (if validation passes)
     * @throws IOException test failure
     * @throws AssertionError Validation failure
     */        
    public DockerInspectImageFacet assertExistsInspectImageFacet
            (@Nonnull String imageId) throws IOException {
        final Fingerprint imageFP = DockerFingerprints.of(imageId);
        assertNotNull(imageFP);
        final DockerInspectImageFacet inspectImageFacet = 
                FingerprintsHelper.getFacet(imageFP, DockerInspectImageFacet.class);
        
        assertNotNull(inspectImageFacet);
        assertEquals(imageId, inspectImageFacet.getData().getId());
        return inspectImageFacet;
    }
            
    private @Nonnull String generateContainerId(@Nonnull String prefix) {
        final String src = "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";
        return prefix + StringUtils.substring(src, 0, 64-prefix.length());
    }
}
