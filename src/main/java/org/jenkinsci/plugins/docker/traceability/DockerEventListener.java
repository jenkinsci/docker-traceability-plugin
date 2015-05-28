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
package org.jenkinsci.plugins.docker.traceability;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerDeploymentFacet;
import org.jenkinsci.plugins.docker.traceability.model.DockerEvent;
import org.jenkinsci.plugins.docker.traceability.model.DockerTraceabilityReport;

/**
 * An extension point, which allows to subscribe to Docker events in Jenkins.
 * @author Oleg Nenashev
 * @since 1.0
 */
public class DockerEventListener implements ExtensionPoint {
    
    /**
     * Notifies external listener that an event in Docker happened.
     * The method does nothing by default
     * @param event Event
     */
    public void onEvent(@Nonnull DockerTraceabilityReport event) {
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
     * Fires {@link DockerEvent} on all listeners.
     * @param event Event to be triggered
     */
    public static void fire(@Nonnull DockerTraceabilityReport event) {
        for (DockerEventListener listener : all()) {
            try {
                listener.onEvent(event);
            } catch (Throwable t) { // Prevent failures on runtime exceptions
                //TODO: logging
            }
        }
    }
    
    /**
     * Notifies all listeners about the container deployment.
     * @param containerId Container ID (full 64-char representation)
     */
    public static void fireNewDeployment(@Nonnull String containerId) {
        for (DockerEventListener listener : all()) {
            try {
                listener.onNewDeployment(containerId);
            } catch (Throwable t) { // Prevent failures on runtime exceptions
                //TODO: logging
            }
        }
    }

    /**
     * Retrieves a list of Docker event listeners.
     * @return A list of all {@link DockerEventListener} extensions.
     */
    public static @Nonnull ExtensionList<DockerEventListener> all() {
        // TODO: null checks
        return Jenkins.getInstance().getExtensionList(DockerEventListener.class);
    }
}
