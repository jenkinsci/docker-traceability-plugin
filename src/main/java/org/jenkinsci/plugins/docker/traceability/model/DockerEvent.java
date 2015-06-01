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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.Event;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import javax.annotation.Nonnull;

/**
 * Describes events happening in Docker.
 * The class can be used as a wrapper of {@link Event} class.
 * @author Oleg Nenashev
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerEvent {

    @JsonProperty
    private final String status; 
    
    @JsonProperty
    private final String id; 
    
    @JsonProperty
    private final String from; 
    
    @JsonProperty
    private final long time;

    public DockerEvent(String status, String id, String from, long time) {
        this.status = status;
        this.id = id;
        this.from = from;
        this.time = time;
    }

    public String getStatus() {
        return status;
    }

    public String getId() {
        return id;
    }

    public String getFrom() {
        return from;
    }

    public long getTime() {
        return time;
    }

    @JsonIgnore
    public Date getDate() {
        return new Date(getTime());
    }
    
    @JsonIgnore
    public @Nonnull DockerEventType getEventType() {
        return DockerEventType.fromString(getStatus());
    }

    /**
     * Raw string describing the event type.
     * @return Event type string in the upper-case format
     */
    @JsonIgnore
    public String getEventTypeStr() {
        return getStatus().toUpperCase(Locale.ENGLISH);
    }
      
    @Override
    public String toString() {
        return getDate() + " " + getId() + " " + getEventTypeStr() + " @ " + getFrom();
    }
    
    /**
     * Converts the local class to the native {@link Event}.
     * @return Docker Event
     * @throws IOException Conversion error
     */
    public @Nonnull Event toDockerEvent() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(this);
        return mapper.readValue(json, Event.class);
    }
}
