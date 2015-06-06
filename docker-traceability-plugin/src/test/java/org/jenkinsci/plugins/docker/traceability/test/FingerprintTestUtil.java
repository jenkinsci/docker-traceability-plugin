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

package org.jenkinsci.plugins.docker.traceability.test;

import hudson.model.AbstractProject;
import hudson.model.Fingerprint;
import hudson.model.Run;
import java.io.IOException;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.docker.commons.fingerprint.DockerFingerprints;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Helper methods for unit tests.
 * @author Oleg Nenashev
 */
public class FingerprintTestUtil {
    
    /**
     * Injects a {@link Fingerprint} and reference facets to the specified run.
     * @param run Run to be modified
     * @param imageId Image id
     * @throws IOException Error
     */
    public static void injectFromFacet (Run run, String imageId) throws IOException {
        DockerFingerprints.addFromFacet(null, imageId, run);
    }
    
    /**
     * A stub method, which emulates the submission of the image reference 
     * from the web interface
     * @param req Incoming request
     * @param rsp Response
     * @param imageId image id
     * @param jobName job name, to which the facet should be attached
     * @throws IOException Request processing error
     * @throws ServletException Servlet error
     */
    public static void doTestSubmitBuildRef(StaplerRequest req, StaplerResponse rsp,
            @QueryParameter(required = true) String imageId,
            @QueryParameter(required = true) String jobName) throws IOException, ServletException {
        final Jenkins j = Jenkins.getInstance();
        if (j == null) {
            throw new IOException("Jenkins instance is not active");
        }
        j.checkPermission(Jenkins.ADMINISTER);
        
        final AbstractProject item = j.getItem(jobName, j, AbstractProject.class);
        final Run latest = item != null ? item.getLastBuild() : null;
        if (latest == null) {
            throw new IOException("Cannot find a project or run to modify"); 
        }
        
        DockerFingerprints.addFromFacet(null,imageId, latest);
        rsp.sendRedirect2(j.getRootUrl());
    }
    
    /**
     * Generates 64-symbol id with the specified prefix;
     * @param prefix Prefix to be retrieved
     * @return Generated ID (works for container and image).
     */
    public static @Nonnull String generateDockerId(@Nonnull String prefix) {
        final String src = "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";
        return prefix + StringUtils.substring(src, 0, 64-prefix.length());
    }
}
