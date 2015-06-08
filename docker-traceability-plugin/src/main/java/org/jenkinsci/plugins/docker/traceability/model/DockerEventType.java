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

import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Describes types of {@link DockerEvent}s.
 * Enum contains all events from
 * <a href="https://docs.docker.com/reference/api/docker_remote_api_v1.16/#monitor-dockers-events">
 * Docker API v.1.16</a>
 * @author Oleg Nenashev
 * @since 1.0
 */
@ExportedBean
public enum DockerEventType {
    /**
     * Container has been created.
     */
    CREATE,
    /**
     * Container has been started.
     */
    START,
    /**
     * Container has been terminated.
     */
    DIE,
    /**
     * Inspect reports (just to update statuses).
     */
    INSPECT_CONTAINER, 
    DESTROY, 
    EXPORT, 
    KILL, 
    PAUSE, 
    RESTART, 
    STOP, 
    UNPAUSE,
    
    // Image events
    UNTAG, 
    DELETE,
    
    /**
     * Indicates there's no event.
     * This event type is being used to update statuses w/o changing the statuses.
     */
    NONE,
    
    /** 
     * Unknown (unregistered) event.
     * Users should not rely on this type in their custom logic, because unknown
     * events may be registered in the enum at some point.
     * In {@link DockerEvent} class {@link DockerEvent#getEventTypeStr()} should
     * be used in such case.
     */
    UNKNOWN;
    
    @SuppressWarnings("null")
    public static @Nonnull DockerEventType fromString(@CheckForNull String str) {
        if (StringUtils.isEmpty(str)) {
            return UNKNOWN;
        }
        
        try {
            return valueOf(str.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
    
    /**
     * 
     * @return true for container events. Also returns true for {@link #UNKNOWN}
     */
    public boolean isContainerEvent() {
        return (this != UNTAG && this != DELETE);
    }
    
    public boolean isImageEvent() {
        return (this == UNTAG || this == DELETE || this == UNKNOWN || this == NONE);
    }
}
