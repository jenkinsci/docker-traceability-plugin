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

import com.github.dockerjava.api.command.InspectImageResponse;
import hudson.model.Action;
import hudson.model.Fingerprint;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.docker.commons.fingerprint.DockerFingerprints;
import org.jenkinsci.plugins.docker.traceability.DockerTraceabilityPlugin;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerContainerRecord;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerDeploymentFacet;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerInspectImageFacet;
import org.jenkinsci.plugins.docker.traceability.model.jobs.DockerBuildReferenceFactory;
import org.jenkinsci.plugins.docker.traceability.model.jobs.DockerBuildReferenceRun;
import org.jenkinsci.plugins.docker.traceability.model.DockerTraceabilityReport;
import org.jenkinsci.plugins.docker.traceability.util.FingerprintsHelper;

/**
 * Provides extra methods, which simplify common traceability use-cases.
 * @author Oleg Nenashev
 */
public class DockerTraceabilityHelper {
    
    private final static Logger LOGGER = Logger.getLogger(DockerTraceabilityPlugin.class.getName());
    private final static String CONTAINER_FP_NAME="<docker-container>";
    
    public static @Nonnull String getImageHash(@Nonnull String imageId) {
        return getFingerprintHash(imageId);
    }
    
    public static @Nonnull String getContainerHash(@Nonnull String containerId) {
        return getFingerprintHash(containerId);
    }
    
    private static @Nonnull String getFingerprintHash(@Nonnull String id) {
        if (id.length() != 64) {
            throw new IllegalArgumentException("Expecting 64char full ID, but got " + id);
        }
        return id.substring(0, 32);
    }
    
    /**
     * Get a fingerprint by the specified container ID.
     * This method allows to handle exception in the logic.
     * Use {@link #of(java.lang.String)} to get a default behavior.
     * @param containerId Full 64-symbol container id. Short forms are not supported.
     * @return Fingerprint. null if it does not exist (or if Jenkins has not been initialized yet)
     * @throws IOException Fingerprint loading error
     */
    public static @CheckForNull Fingerprint ofValidated(@Nonnull String containerId) throws IOException {
        final Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            return null;
        }
        return jenkins.getFingerprintMap().get(getContainerHash(containerId));
    }
    
    /**
     * Get a fingerprint by the specified container ID.
     * Logs and ignores {@link IOException}s on fingerprint loading.
     * @param containerId Full 64-symbol container id. Short forms are not supported.
     * @return Fingerprint. null if it is not available or if a loading error happens
     */
    public static @CheckForNull Fingerprint of(@Nonnull String containerId) {
        try {
            return ofValidated(containerId);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Cannot load fingerprint for containerId=" + containerId, ex);
            return null;
        }
    }
    
    /**
     * Get or create a fingerprint by the specified container ID.
     * @param containerId Full 64-symbol container id. Short forms are not supported.
     * @param name Optional container name
     * @param timestamp Timestamp if there is a need to create a new container
     * @return Fingerprint. null if Jenkins has not been initialized yet
     * @throws IOException Fingerprint loading error
     */
    public static @CheckForNull Fingerprint make(@Nonnull String containerId, 
            @CheckForNull String name, long timestamp) throws IOException {
        final Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            return null;
        }
        
        final DockerBuildReferenceRun run = DockerBuildReferenceFactory.forContainer(containerId, name, timestamp);
        final Fingerprint fp = jenkins.getFingerprintMap().getOrCreate(
                run, "Container "+(name != null ? name : name), getContainerHash(containerId));
        return fp;
    }
    
    /**
     * Get or create a fingerprint by the specified image ID.
     * @param imageId Full 64-symbol image id. Short forms are not supported.
     * @param name Optional container name
     * @param timestamp Timestamp if there is a need to create a new image
     * @return Fingerprint. null if Jenkins has not been initialized yet
     * @throws IOException Fingerprint loading error
     */
    public static @CheckForNull Fingerprint makeImage(@Nonnull String imageId, 
            @CheckForNull String name, long timestamp) throws IOException {
        final Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            return null;
        }
        
        final DockerBuildReferenceRun run = DockerBuildReferenceFactory.forImage(imageId, name, timestamp);
        final Fingerprint fp = jenkins.getFingerprintMap().getOrCreate(
                run, "Image "+(name != null ? name : name), getImageHash(imageId));
        return fp;
    }
    
    /**
     * Retrieves the last deployment record for the specified container.
     * @param containerId Container Id
     * @return Last registered record. Null if there is no data available
     *         (or if an internal exception happens)
     */
    public static @CheckForNull DockerContainerRecord getLastContainerRecord(@Nonnull String containerId) {
        final Fingerprint fp = of(containerId);
         if (fp != null) {
            final DockerDeploymentFacet facet = FingerprintsHelper.getFacet(fp, DockerDeploymentFacet.class);
            if (facet != null) {
                return facet.getLatest();
            }
        }
        return null;
    }    
    
    /**
     * Retrieves the last trace report for the specified container.
     * @param containerId Container Id
     * @return Last available report. Null if there is no data available
     *         (or if an internal exception happens)
     */
    public static @CheckForNull DockerTraceabilityReport getLastReport(@Nonnull String containerId) {
        final DockerContainerRecord record = getLastContainerRecord(containerId);
        return (record != null) ? record.getReport() : null;
    }
    
    /**
     * Retrieves the last {@link InspectImageResponse} for the specified image.
     * @param imageId Image Id
     * @return Last available report. Null if there is no data available
     *         (or if an internal exception happens)
     */
    public static @CheckForNull InspectImageResponse getLastInspectImageResponse(@Nonnull String imageId) {
        try {
            final Fingerprint fp = DockerFingerprints.of(imageId);
            if (fp != null) {
                final DockerInspectImageFacet facet = FingerprintsHelper.getFacet(fp, DockerInspectImageFacet.class);
                if (facet != null) {
                    return facet.getData();
                }
            }
            return null;
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Cannot retrieve deployment reports for imageId="+imageId, ex);
            return null;
        }
    }
    
    
    
    /**
     * Formats the time to the Docker-standard format.
     * @param time Time to be converted
     * @return String in the following format: {@code yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'} 
     *      Up to nanoseconds, but actually the last 6 digits are null in the current implementation.
     */
    public static @Nonnull String formatTime(long time) {
        return formatDate(new Date (time));
    }
    
    /**
     * Formats the time to the Docker-standard format.
     * @param date Date to be converted
     * @return String in the following format: {@code yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'} 
     *      Up to nanoseconds, but actually the last 6 digits are null in the current implementation.
     */
    public static @Nonnull String formatDate(Date date) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        df.setTimeZone(tz);
        return df.format(date)+"000000Z"; 
    }
}
