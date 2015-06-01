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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.FingerprintFacet;
import org.jenkinsci.plugins.docker.commons.fingerprint.DockerFingerprintFacet;
import org.jenkinsci.plugins.docker.traceability.core.DockerTraceabilityHelper;
import org.jenkinsci.plugins.docker.traceability.model.DockerEventType;
import org.jenkinsci.plugins.docker.traceability.model.DockerTraceabilityReport;
import org.jenkinsci.plugins.docker.traceability.util.FingerprintsHelper;

/**
 * Implements a facet for {@link DockerContainerRecord}s.
 * It's supposed to be used within a container fingerprint, which stores
 * the info about events.
 * This facet should be added to container {@link Fingerprint}s.
 * @author Oleg Nenashev
 */
public class DockerDeploymentFacet extends DockerFingerprintFacet {
       
    private final List<DockerContainerRecord> deploymentRecords 
            = new ArrayList<DockerContainerRecord>();

    public DockerDeploymentFacet(Fingerprint fingerprint, long timestamp) {
        super(fingerprint, timestamp);
    }

    public synchronized void add(DockerContainerRecord r) throws IOException {
        for (DockerContainerRecord e : deploymentRecords) { // prevent dups
            if (e.equals(r))
                return;
        }
        deploymentRecords.add(r);
        getFingerprint().save();
    }
    
    public synchronized @Nonnull List<DockerContainerRecord> getDeploymentRecords() {
        return new ArrayList<DockerContainerRecord>(deploymentRecords);
    }

    public synchronized @CheckForNull DockerContainerRecord getLatest() {
        return (deploymentRecords.isEmpty()) ? null : deploymentRecords.get(deploymentRecords.size()-1);
    }
    
    /**
     * Retrieves the last known status.
     * @return Status string
     */
    public synchronized @Nonnull String getLastStatus() {
        String lastStatus = null;
        for (DockerContainerRecord record : deploymentRecords) {
            String recordStatus = record.getReport().getEvent().getStatus();
            DockerEventType status = DockerEventType.fromString(recordStatus);
            if (status != DockerEventType.NONE) { // Yes, we accept Unknown statuses frow new Docker versions
                lastStatus = recordStatus.toUpperCase(Locale.ENGLISH);
            }
        }
        return (lastStatus != null) ? lastStatus : DockerEventType.UNKNOWN.toString();
    }
    
    private DockerDeploymentFacet(@Nonnull Fingerprint fingerprint) {
        //TODO: what to do with the timestamp?
        super(fingerprint, 0);
    }
  
    /**
     * Retrieves a deployment facet for the specified container.
     * @param containerId Container ID (64-char)
     * @return Facet. Null if it is not available
     */
    public static @CheckForNull DockerDeploymentFacet getDeploymentFacet(String containerId) {      
        Fingerprint fp = DockerTraceabilityHelper.of(containerId);
        if (fp == null) {
            return null;
        }
        return FingerprintsHelper.getFacet(fp, DockerDeploymentFacet.class);     
    }
    
    public static @Nonnull DockerDeploymentFacet getOrCreate(@Nonnull Fingerprint fingerprint)
            throws IOException {  
        // Try to find an existing facet
        for ( FingerprintFacet facet : fingerprint.getFacets()) {
            if (facet instanceof DockerDeploymentFacet) {
                return (DockerDeploymentFacet) facet;
            }
        }
        
        // Create new one
        DockerDeploymentFacet facet = new DockerDeploymentFacet(fingerprint);
        fingerprint.getFacets().add(facet);
        fingerprint.save();
        return facet;
    }
    
    public static DockerDeploymentFacet addEvent(@Nonnull Fingerprint fingerprint, @Nonnull DockerTraceabilityReport event) 
            throws IOException {    
        DockerDeploymentFacet facet = getOrCreate(fingerprint);
        facet.add(new DockerContainerRecord(event));
        fingerprint.save();
        return facet;
    }
}
