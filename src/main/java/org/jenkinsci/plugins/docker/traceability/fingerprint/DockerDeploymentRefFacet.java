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

import hudson.model.Fingerprint;
import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.FingerprintFacet;
import org.jenkinsci.plugins.docker.commons.fingerprint.DockerFingerprintFacet;
import org.jenkinsci.plugins.docker.traceability.core.DockerTraceabilityHelper;
import org.jenkinsci.plugins.docker.traceability.model.DockerEventType;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * References containers using the image.
 * This facet should be added to image {@link Fingerprint}s.
 * @author Oleg Nenashev
 */
public class DockerDeploymentRefFacet extends DockerFingerprintFacet {
       
    private final @Nonnull Set<String> containerIds = new TreeSet<String>();

    public DockerDeploymentRefFacet(@Nonnull Fingerprint fingerprint, long timestamp) {
        super(fingerprint, timestamp);
    }

    public synchronized @Nonnull Set<String> getContainerIds() {
        return new TreeSet<String>(containerIds);
    }
    
    /**
     * Gets a last container record for every container.
     * @param containerId Container ID (64-char string)
     * @return Last container record
     */
    @Restricted(NoExternalUse.class)
    public synchronized @CheckForNull DockerContainerRecord getLastRecord(@Nonnull String containerId) {
        return DockerTraceabilityHelper.getLastContainerRecord(containerId);
    }
    
    /**
     * Retrieves the last known status.
     * @param containerId Container ID (64-char string)
     * @return Status string
     */
    public synchronized @Nonnull String getLastStatus(@Nonnull String containerId) {
        DockerDeploymentFacet deploymentFacet = DockerDeploymentFacet.getDeploymentFacet(containerId);
        return (deploymentFacet != null) 
                ? deploymentFacet.getLastStatus() : DockerEventType.UNKNOWN.toString();
    }

    private synchronized void addRef(@Nonnull String containerId) {
        containerIds.add(containerId);
    }

    public static @Nonnull DockerDeploymentRefFacet getOrCreate(@Nonnull Fingerprint fingerprint, long timestamp) throws IOException {  
        // Try to find an existing facet with the same 
        for ( FingerprintFacet facet : fingerprint.getFacets()) {
            if (facet instanceof DockerDeploymentRefFacet) {
                return (DockerDeploymentRefFacet) facet;
            }
        }
        
        // Create new one
        DockerDeploymentRefFacet facet = new DockerDeploymentRefFacet(fingerprint, timestamp);
        fingerprint.getFacets().add(facet);
        fingerprint.save();
        return facet;
    }
    
    public static DockerDeploymentRefFacet addRef(@Nonnull Fingerprint fingerprint, @Nonnull String containerId) 
            throws IOException {    
        DockerDeploymentRefFacet facet = getOrCreate(fingerprint, new Date().getTime());
        facet.addRef(containerId);
        fingerprint.save();
        return facet;
    }
}
