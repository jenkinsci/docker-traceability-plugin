/*
 * The MIT License
 *
 * Copyright (c) 2015 CloudBees, Inc.
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

import java.util.LinkedList;
import org.jenkinsci.plugins.docker.commons.fingerprint.DockerFingerprints;
import org.jenkinsci.plugins.docker.traceability.api.DockerTraceabilityReport;
import org.jenkinsci.plugins.docker.traceability.DockerTraceabilityPluginConfiguration;
import org.jenkinsci.plugins.docker.traceability.DockerTraceabilityPluginTest;
import org.jenkinsci.plugins.docker.traceability.dockerjava.api.command.InspectContainerResponse;
import org.jenkinsci.plugins.docker.traceability.dockerjava.api.model.Event;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerDeploymentFacet;
import org.jenkinsci.plugins.docker.traceability.model.DockerEvent;
import org.jenkinsci.plugins.docker.traceability.model.DockerTraceabilityReportListener;
import org.jenkinsci.plugins.docker.traceability.samples.JSONSamples;
import org.jenkinsci.plugins.docker.traceability.test.FingerprintTestUtil;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Tests for {@link DockerTraceabilityRootAction}.
 * @author Oleg Nenashev
 */
public class DockerTraceabilityReportListenerImplTest {
    
    @Rule
    public JenkinsRule j = new JenkinsRule();
    
    /**
     * Checks the resolution of imageId from previously saved data.
     * @throws Exception test failure
     */
    @Bug(28752)
    public void submitReportWithoutImageReference() throws Exception {
        DockerTraceabilityPluginTest.configure(new DockerTraceabilityPluginConfiguration(true, true));
        final String imageId = FingerprintTestUtil.generateDockerId("1");
        final Event event1 = new DockerEvent("run", imageId, "host", 12345).toDockerEvent();
        final Event event2 = new DockerEvent("die", imageId, "host", 12346).toDockerEvent();
        final InspectContainerResponse containerInfo = JSONSamples.inspectContainerData_emptyImage.
                readObject(InspectContainerResponse.class);
        
        DockerTraceabilityReport r1 = new DockerTraceabilityReport(event1, null, containerInfo, imageId, null, null, 
                new LinkedList<String>(), null);
        DockerTraceabilityReport r2 = new DockerTraceabilityReport(event2, null, containerInfo, null, null, null, 
                new LinkedList<String>(), null);
        
        // Spawn two reports
        DockerTraceabilityReportListener.fire(r1);
        DockerTraceabilityReportListener.fire(r2);
        
        // Retrieve
        final DockerDeploymentFacet facet = 
            DockerFingerprints.getFacet(containerInfo.getId(), DockerDeploymentFacet.class);
        assertNotNull(facet);
        assertEquals(imageId, facet.getImageId());
        assertEquals("Expected both reports to be saved in the fingerprint", 2, facet.getDeploymentRecords().size());     
    }
    
}
