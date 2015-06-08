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
package org.jenkinsci.plugins.docker.traceability.fingerprint;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Event;
import java.io.Serializable;
import java.util.Comparator;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.docker.traceability.core.DockerTraceabilityHelper;
import org.jenkinsci.plugins.docker.traceability.model.DockerTraceabilityReport;

/**
 * Stores an entry for {@link DockerDeploymentFacet}.
 * @author Oleg Nenashev
 */
public class DockerContainerRecord {

    private final @Nonnull DockerTraceabilityReport report;
    
    public DockerContainerRecord(@Nonnull DockerTraceabilityReport report) {
        this.report = report;
    }
    
    /**
     * Gets the nested report.
     * @return A related {@link DockerTraceabilityReport}
     */
    public @Nonnull DockerTraceabilityReport getReport() {
        return report;
    }
    
    public @CheckForNull String getContainerId() {
        InspectContainerResponse container = report.getContainer();
        return (container != null) ? container.getId() : null;
    }
    
    public @CheckForNull String getContainerFingerprintHash() {
        InspectContainerResponse container = report.getContainer();
        return (container != null) ? DockerTraceabilityHelper.getContainerHash(container.getId()) : null;
    }
    
    public @CheckForNull String getImageFingerprintHash() {
        final String imageId = report.getImageId();
        return imageId != null ? DockerTraceabilityHelper.getImageHash(imageId) : null;
    }
    
    /**
     * Compares {@link DockerContainerRecord}s by time
     */
    public static class TimeComparator implements Comparator<DockerContainerRecord>, Serializable {
        
        private static final long serialVersionUID = 1L;
        
        public int compare(DockerContainerRecord o1, DockerContainerRecord o2) {
            
            final Event event1 = o1.getReport().getEvent();
            final Event event2 = o2.getReport().getEvent();
            if (event1.getTime() != event2.getTime()) {
                return Long.compare(event1.getTime(), event2.getTime());
            }
            
            // We rely on the event type and presume there's no similar events
            // at the same time
            return event1.getStatus().compareTo(event2.getStatus());
        }
    }
}
