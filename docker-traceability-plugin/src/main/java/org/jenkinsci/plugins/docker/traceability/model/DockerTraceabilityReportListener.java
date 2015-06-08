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
package org.jenkinsci.plugins.docker.traceability.model;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.docker.traceability.DockerTraceabilityPlugin;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerDeploymentFacet;
import org.jenkinsci.plugins.docker.traceability.model.DockerEvent;
import org.jenkinsci.plugins.docker.traceability.model.DockerTraceabilityReport;

/**
 * An extension point, which allows to subscribe to {@link DockerTraceabilityReport}s in Jenkins.
 * @author Oleg Nenashev
 * @since 1.0
 */
public class DockerTraceabilityReportListener implements ExtensionPoint {
    
    private final static Logger LOGGER = Logger.getLogger(DockerTraceabilityPlugin.class.getName());
    
    /**
     * Notifies external listeners that a new Docker report has been received.
     * The method does nothing by default
     * @param report Event
     */
    public void onReport(@Nonnull DockerTraceabilityReport report) {
        // Do nothing by default
    }
    
    /**
     * Being called when a container deployment has been spotted.
     * Common case: registration of new {@link DockerDeploymentFacet}.
     * @param containerId Container ID (full 64-char representation)
     */
    public void onNewDeployment(@Nonnull String containerId) {
        // Do nothing by default
    }
    
    /**
     * Process {@link DockerTraceabilityReport} on all listeners.
     * @param report Event to be triggered
     */
    public static void fire(@Nonnull DockerTraceabilityReport report) {
        for (DockerTraceabilityReportListener listener : all()) {
            try {
                listener.onReport(report);
            } catch (Throwable t) { // Prevent failures on runtime exceptions
                LOGGER.log(Level.SEVERE, "Runtime exception during the event processing in "+ listener, t);
            }
        }
    }
    
    /**
     * Notifies all listeners about the container deployment.
     * @param containerId Container ID (full 64-char representation)
     */
    public static void fireNewDeployment(@Nonnull String containerId) {
        for (DockerTraceabilityReportListener listener : all()) {
            try {
                listener.onNewDeployment(containerId);
            } catch (Throwable t) { // Prevent failures on runtime exceptions
                LOGGER.log(Level.SEVERE, "Runtime exception during the new deployment processing in "+ listener, t);
            }
        }
    }

    /**
     * Retrieves a list of Docker event listeners.
     * @return A list of all {@link DockerTraceabilityReportListener} extensions.
     */
    public static @Nonnull ExtensionList<DockerTraceabilityReportListener> all() {
        final Jenkins j = Jenkins.getInstance();
        if (j == null) {
            return ExtensionList.create((Jenkins) null, DockerTraceabilityReportListener.class);
        }
        return j.getExtensionList(DockerTraceabilityReportListener.class);
    }
}
