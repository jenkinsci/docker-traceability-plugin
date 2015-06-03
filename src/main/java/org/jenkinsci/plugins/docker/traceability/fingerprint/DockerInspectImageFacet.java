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

import com.github.dockerjava.api.command.InspectImageResponse;
import hudson.model.Fingerprint;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.FingerprintFacet;
import org.jenkinsci.plugins.docker.commons.fingerprint.DockerFingerprintFacet;
import org.jenkinsci.plugins.docker.traceability.util.FingerprintsHelper;

/**
 * Stores the info about image in {@link Fingerprint}s.
 * Stores only the last available data in order to display summaries in the web
 * interface. The consistency of the container info in {@link #data} is not 
 * maintained.
 * @author Oleg Nenashev
 */
public class DockerInspectImageFacet extends DockerFingerprintFacet {
    
    /**
     * {@link #data} submission time.
     * Format: seconds since January 1, 1970, 00:00:00 GMT
     */
    private long reportTimeInSeconds;
    private @CheckForNull String imageName;
    private InspectImageResponse data;
    
    public DockerInspectImageFacet(@Nonnull Fingerprint fingerprint, long timestamp,
           @Nonnull InspectImageResponse data, @Nonnull String imageName) {
        super(fingerprint, timestamp);  
        this.data = data;
        this.reportTimeInSeconds = timestamp;
        this.imageName = hudson.Util.fixEmpty(imageName);
    } 

    public @Nonnull InspectImageResponse getData() {
        return data;
    }

    /**
     * Gets the image name.
     * This name is an optional field.
     * @return A human-readable image name
     */
    public @CheckForNull String getImageName() {
        return imageName;
    }
    
    /**
     * Time, when the latest report has been submitted.
     * @return The time is specified in seconds since January 1, 1970, 00:00:00 GMT
     */
    public long getReportTimeInSeconds() {
        return reportTimeInSeconds;
    }
    
    private void updateData(@Nonnull InspectImageResponse data, long reportTimeInSeconds,
            @CheckForNull String imageName) throws IOException {
        if (reportTimeInSeconds > reportTimeInSeconds) {
            this.data = data;
            this.reportTimeInSeconds = reportTimeInSeconds;         
        }
        this.imageName = hudson.Util.fixEmpty(imageName);
    }
    
    /**
     * Updates the facet by a new report.
     * The submission will be ignored if the current {@link #reportTimeInSeconds} is 
     * greater than the submitted one,
     * @param fingerprint Fingerprint to be updated
     * @param reportTimeInSeconds Report generation time.
     *      The time is specified in seconds since January 1, 1970, 00:00:00 GMT
     * @param data Report data from &quot;docker inspect image&quot; output
     * @param imageName Optional name of the image
     * @throws IOException Fingerprint save error
     */
    public static void updateData(@Nonnull Fingerprint fingerprint, long reportTimeInSeconds, 
            @Nonnull InspectImageResponse data, @CheckForNull String imageName) throws IOException {       
        DockerInspectImageFacet facet = FingerprintsHelper.getFacet(fingerprint, 
                DockerInspectImageFacet.class);
        if (facet == null) {
            facet = new DockerInspectImageFacet(fingerprint, reportTimeInSeconds, data, imageName);
            fingerprint.getFacets().add(facet);
        } else {
           facet.updateData(data, reportTimeInSeconds, imageName);
        }
        fingerprint.save();
    }
}
