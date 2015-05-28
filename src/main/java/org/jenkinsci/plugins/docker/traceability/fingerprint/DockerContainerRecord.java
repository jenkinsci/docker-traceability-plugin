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
import java.io.Serializable;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.docker.traceability.core.DockerTraceabilityHelper;
import org.jenkinsci.plugins.docker.traceability.model.DockerEvent;
import org.jenkinsci.plugins.docker.traceability.model.DockerTraceabilityReport;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Stores an entry for {@link DockerDeploymentFacet}.
 * @author Oleg Nenashev
 */
@ExportedBean
public class DockerContainerRecord implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private final @Nonnull DockerTraceabilityReport event;
    
    public DockerContainerRecord(@Nonnull DockerTraceabilityReport event) {
        this.event = event;
    }
    
    /**
     * Refers a {@link DockerEvent}, from which we got an info regarding the deployment
     * of the container.
     * @return A related {@link DockerEvent}
     */
    @Exported
    public @Nonnull DockerTraceabilityReport getEvent() {
        return event;
    }
    
    public @CheckForNull String getContainerId() {
        InspectContainerResponse container = event.getContainer();
        return (container != null) ? container.getId() : null;
    }
    
    public @CheckForNull String getContainerFingerprintHash() {
        InspectContainerResponse container = event.getContainer();
        return (container != null) ? DockerTraceabilityHelper.getContainerHash(container.getId()) : null;
    }
    
    public @Nonnull String getImageFingerprintHash() {
        return DockerTraceabilityHelper.getImageHash(event.getImageId());
    }
}
