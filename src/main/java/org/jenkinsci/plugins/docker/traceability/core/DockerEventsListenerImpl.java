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

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerDeploymentFacet;
import hudson.Extension;
import hudson.model.Fingerprint;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.docker.commons.fingerprint.DockerFingerprints;
import org.jenkinsci.plugins.docker.traceability.DockerEventListener;
import org.jenkinsci.plugins.docker.traceability.model.DockerTraceabilityReport;
import org.jenkinsci.plugins.docker.traceability.DockerTraceabilityPlugin;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerDeploymentRefFacet;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerInspectImageFacet;

/**
 * Listens for {@link DockerTraceabilityReport}s and pushes them to fingerprints.
 * @author Oleg Nenashev
 */
@Extension
public class DockerEventsListenerImpl extends DockerEventListener {

    private final static Logger LOGGER = Logger.getLogger(DockerTraceabilityPlugin.class.getName());
    
    @Override
    public void onEvent(DockerTraceabilityReport event) {
        final String imageId = event.getImageId();
        LOGGER.log(Level.FINE, "Got an event for image {0}", imageId);       
        
        try {
            processEvent(event);
        } catch (Throwable ex) { // Catch everything
            LOGGER.log(Level.WARNING, "Cannot retrieve the fingerprint", ex);
        } 
    }   
    
    private void processEvent(@Nonnull DockerTraceabilityReport report) throws IOException {
        // Get fingerprints for the image
        final Fingerprint imageFP = DockerFingerprints.of(report.getImageId());
        if (imageFP == null) {
            LOGGER.log(Level.FINE, "Cannot find a fingerprint for image {0}. "
                 + "Most probably, the image has not been created in Jenkins. Event will be ignored", report.getImageId());
            return;
        }
               
        // Update containerInfo if available
        final InspectContainerResponse containerInfo = report.getContainer();
        @CheckForNull Fingerprint containerFP = null;
        if (containerInfo != null) {
            final String containerId = containerInfo.getId();
            containerFP = DockerTraceabilityHelper.make(containerId);
            if (containerFP != null) {
                DockerDeploymentFacet.addEvent(containerFP, report);
                DockerDeploymentRefFacet.addRef(imageFP, containerInfo.getId());
            } else {
                LOGGER.log(Level.WARNING, "Cannot retrieve the fingerprint for containerId={0}", containerInfo.getId());
            }
            // Notify listeners
            DockerEventListener.fireNewDeployment(containerId);
        }
        
        // Update image facets by a new info if available
        final InspectImageResponse imageInfo = report.getImage();
        if (imageInfo != null) {
            DockerInspectImageFacet.updateData(imageFP, report.getEvent().getTime(), 
                    imageInfo, report.getImageName());         
        }
        
        // Process other commands when it is required
    }

    @Override
    public void onNewDeployment(String containerId) {
        final DockerTraceabilityRootAction action = DockerTraceabilityHelper.getTraceabilityAction();
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
