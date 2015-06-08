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

import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerDeploymentFacet;
import hudson.Extension;
import hudson.model.Fingerprint;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.docker.commons.fingerprint.DockerFingerprints;
import org.jenkinsci.plugins.docker.traceability.model.DockerTraceabilityReportListener;
import org.jenkinsci.plugins.docker.traceability.api.DockerTraceabilityReport;
import org.jenkinsci.plugins.docker.traceability.DockerTraceabilityPlugin;
import org.jenkinsci.plugins.docker.traceability.dockerjava.api.command.InspectContainerResponse;
import org.jenkinsci.plugins.docker.traceability.dockerjava.api.command.InspectImageResponse;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerDeploymentRefFacet;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerInspectImageFacet;

/**
 * Listens for {@link DockerTraceabilityReport}s and pushes them to fingerprints.
 * @author Oleg Nenashev
 */
@Extension
public class DockerTraceabilityReportListenerImpl extends DockerTraceabilityReportListener {

    private final static Logger LOGGER = Logger.getLogger(DockerTraceabilityPlugin.class.getName());
    
    @Override
    public void onReport(DockerTraceabilityReport report) {
        final String imageId = report.getImageId();
        LOGGER.log(Level.FINE, "Got an event for image {0}", imageId);       
        
        try {
            processReport(report);
        } catch (Throwable ex) { // Catch everything
            LOGGER.log(Level.WARNING, "Cannot retrieve the fingerprint", ex);
        } 
    }   
    
    private void processReport(@Nonnull DockerTraceabilityReport report) throws IOException {
        DockerTraceabilityPlugin plugin = DockerTraceabilityPlugin.getInstance();

        String imageId = report.getImageId();
        if (imageId == null) { // Try to restore imageId by container id
            String containerId = report.getContainerId();
            if (containerId != null) {
                final Fingerprint containerFP = DockerFingerprints.of(containerId);
                final DockerDeploymentFacet facet = containerFP != null
                        ? DockerFingerprints.getFacet(containerFP, DockerDeploymentFacet.class)
                        : null;
                imageId = facet != null ? facet.getImageId() : null;
            }
        }
        
        // Get fingerprints for the image
        Fingerprint imageFP = null;
        if (imageId != null) {
            imageFP = DockerFingerprints.of(imageId);
            if (imageFP == null && plugin.getConfiguration().isCreateImageFingerprints()) {
                LOGGER.log(Level.FINE, "Creating a new fingerprint for image {0}", report.getImageId());
                imageFP = DockerTraceabilityHelper.makeImage(imageId, 
                        report.getImageName(), report.getEvent().getTime());
            }
        
            if (imageFP == null) { // We don't create anything and exit
                LOGGER.log(Level.FINE, "Cannot get or create a fingerprint for image {0}. "
                    + "Most probably, the image has not been created in Jenkins. Report will be ignored", 
                        report.getImageId());
                return;
            }
        }
        
        if (imageId == null) { // Add the low-important warning
            LOGGER.log(Level.FINE, "Cannot retrieve the imageId for container {0}. "
                    + "Image fingerprints won't be created", report.getContainerId());
        }
               
        // Update containerInfo if available
        final InspectContainerResponse containerInfo = report.getContainer();
        if (containerInfo != null) {
            final String containerId = containerInfo.getId();
            final String containerName = hudson.Util.fixEmptyAndTrim(containerInfo.getName());
            final Fingerprint containerFP = DockerTraceabilityHelper.make(containerId, containerName);
            if (containerFP != null) {
                DockerDeploymentFacet.addEvent(containerFP, report);
                if (imageFP != null) {
                    DockerDeploymentRefFacet.addRef(imageFP, containerInfo.getId());
                }
            } else {
                LOGGER.log(Level.WARNING, "Cannot retrieve the fingerprint for containerId={0}", containerInfo.getId());
            }
            // Notify listeners
            DockerTraceabilityReportListener.fireNewDeployment(containerId);
        }
        
        // Update image facets by a new info if available
        final InspectImageResponse imageInfo = report.getImage();
        if (imageInfo != null && imageFP != null) {
            DockerInspectImageFacet.updateData(imageFP, report.getEvent().getTime(), 
                    imageInfo, report.getImageName());         
        }
        
        // Process other commands when it is required
    }

    @Override
    public void onNewDeployment(String containerId) {
        final DockerTraceabilityRootAction action = DockerTraceabilityRootAction.getInstance();
        if (action == null) {
            return; // Hopefully we'll register the container later
        }
        try {
            action.addContainerID(containerId);
        } catch(IOException ex) {
            LOGGER.log(Level.SEVERE, "Cannot save an info about newly registered containerId="+containerId, ex);
        }
    }

}
