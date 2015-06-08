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
import com.github.dockerjava.api.model.Info;
import java.util.ArrayList;
import java.util.Collections;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.lang.StringUtils;

@JsonIgnoreProperties(ignoreUnknown=true)
public class DockerTraceabilityReport {

    @JsonProperty
    private @Nonnull Event event;
    
    @JsonProperty
    private @Nonnull Info hostInfo;
    
    @JsonProperty(required = false)
    private @CheckForNull InspectContainerResponse container;
    
    @JsonProperty(required = false)
    private @CheckForNull InspectImageResponse image;
    
    @JsonProperty(required = false)
    private @CheckForNull String imageId;
    
    @JsonProperty(required = false)
    private @CheckForNull String imageName;
    
    @JsonProperty(required = false)
    private @CheckForNull String environment;
    
    @JsonProperty
    private @Nonnull List<String> parents;
      
    /**
     * Stub constructor for deserialization purposes.
     */
    public DockerTraceabilityReport() {
    }
    
    public DockerTraceabilityReport(@Nonnull Event event, @Nonnull Info hostInfo, 
            @CheckForNull InspectContainerResponse container, 
            @CheckForNull String imageId, @CheckForNull String imageName, 
            @CheckForNull InspectImageResponse image,
            @Nonnull List<String> parents, @CheckForNull String environment) {
        this.event = event;
        this.hostInfo = hostInfo;
        this.container = container;
        this.imageId = imageId;
        this.image = image;
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
    
    /**
     * Gets ID of the image.
     * The method will try to retrieve the ID from several places in the report.
     * @return Full 64-symbol IDs are supported. May be null in corner cases
     * when the image info becomes deleted before the report submission.
     */
    public @CheckForNull String getImageId() {
        if (imageId != null) {
            return imageId;
        }
        
        // Try InspectImageResponse
        if (image != null) {
            final String idFromImage = image.getId();
            if (StringUtils.isNotBlank(idFromImage)) {
                return idFromImage;
            }
        }
        
        // Try InspectContainerResponse
        if (container != null) {
            final String idFromContainer = container.getImageId();
            if (StringUtils.isNotBlank(idFromContainer)) {
                return idFromContainer;
            }
        }
        
        // TODO: Try extracting from event, which may have the data in some cases?
        return null;
    }
    
    /**
     * Gets ID of the container.
     * The method will try to retrieve the ID from several places in the report.
     * @return Full 64-symbol IDs are supported. May be null if there is no 
     * data in the request.
     */
    public @CheckForNull String getContainerId() {
        if (container != null) {
            final String idFromContainer = container.getId();
            if (StringUtils.isNotBlank(idFromContainer)) {
                return idFromContainer;
            }
        }
        
        // TODO: Try other possible sources like event?
        return null;
    }

    /**
     * The ordered list of parents (parents first, then grandparents etc.)
     * @return the list of parents image IDs.
     */
    public @Nonnull List<String> getParents() {
        return Collections.unmodifiableList(parents);
    }
    
}
