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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.Info;import java.util.ArrayList;
import java.util.Collections;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

@JsonIgnoreProperties(ignoreUnknown=true)
public class DockerTraceabilityReport {

    @JsonProperty
    private Event event;
    
    @JsonProperty
    private Info hostInfo;
    
    @JsonProperty(required = false)
    private InspectContainerResponse container;
    
    @JsonProperty(required = false)
    private InspectImageResponse image;
    
    @JsonProperty
    private String imageId;
    
    @JsonProperty(required = false)
    private @CheckForNull String imageName;
    
    @JsonProperty(required = false)
    private @CheckForNull String environment;
    
    @JsonProperty
    private List<String> parents;
      
    /**
     * Stub constructor for deserialization purposes.
     */
    public DockerTraceabilityReport() {
    }
    
    public DockerTraceabilityReport(@Nonnull Event event, @Nonnull Info hostInfo, 
            @CheckForNull InspectContainerResponse container, 
            @Nonnull String imageId, @CheckForNull String imageName, 
            @Nonnull List<String> parents, @CheckForNull String environment) {
        this.event = event;
        this.hostInfo = hostInfo;
        this.container = container;
        this.imageId = imageId;
        this.parents = new ArrayList<String>(parents);
        this.imageName = imageName;
        this.environment = environment;
        
    }

    public @Nonnull Event getEvent() {
        return event;
    }

    public @Nonnull Info getHostInfo() {
        return hostInfo;
    }

    public @CheckForNull String getEnvironment() {
        return environment;
    }

    public @CheckForNull String getImageName() {
        return imageName;
    }
 
    /**
     * Optional info about container.
     * @return Container info. Null if there was no info in the submitted report.
     */
    public @CheckForNull InspectContainerResponse getContainer() {
        return container;
    }

    /**
     * Optional info about image.
     * @return Image info. Null if there was no info in the submitted report.
     */
    public @CheckForNull InspectImageResponse getImage() {
        return image;
    }
    
    public @Nonnull String getImageId() {
        return imageId;
    }

    /**
     * The ordered list of parents (parents first, then grandparents etc.)
     * @return the list of parents image IDs.
     */
    public @Nonnull List<String> getParents() {
        return Collections.unmodifiableList(parents);
    }
    
}
