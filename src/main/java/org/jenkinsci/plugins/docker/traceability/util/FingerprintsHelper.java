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
package org.jenkinsci.plugins.docker.traceability.util;


import hudson.model.AbstractProject;
import hudson.model.Fingerprint;
import hudson.model.Run;
import hudson.util.RunList;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.FingerprintFacet;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.docker.commons.fingerprint.DockerFingerprintAction;

/**
 * Common methods for {@link Fingerprint}s.
 * @author Oleg Nenashev
 */
public class FingerprintsHelper {
    
    public static @CheckForNull @SuppressWarnings("unchecked")
            <TFacet extends FingerprintFacet> TFacet getFacet
            (@Nonnull Fingerprint fingerprint, @Nonnull Class<TFacet> facetClass) {  
        for ( FingerprintFacet facet : fingerprint.getFacets()) {
            if (facetClass.isAssignableFrom(facet.getClass())) {
                return (TFacet)facet;
            }
        }
        return null;      
    }
             
    public static void addFacet(@Nonnull Fingerprint fingerprint, @Nonnull FingerprintFacet facet)
           throws IOException {
        fingerprint.getFacets().add(facet);
        fingerprint.save();
    }
    
    /**
     * Get all imageIDs, which have fingerprints somewhere. 
     * @return Collection of image IDs, which have declared fingerprints.
     */
    public static Set<String> getImagesWithFingerprints() {
        Set<String> result = new HashSet<String>();

        //TODO: Just4test, horrible performance, we need caching
        for (AbstractProject item : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
            RunList<Run> runs = item.getBuilds();
            for (Run run : runs) {
                DockerFingerprintAction action = run.getAction(DockerFingerprintAction.class);
                if (action != null) {
                    result.addAll(action.getImageIDs());
                }
            }
        }
        return result;
    }
}
