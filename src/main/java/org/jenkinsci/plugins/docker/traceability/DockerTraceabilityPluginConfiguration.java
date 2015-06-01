/*
 * The MIT License
 *
 * Copyright (c) 2015 Oleg Nenashev.
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
package org.jenkinsci.plugins.docker.traceability;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import javax.annotation.Nonnull;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Configuration of {@link DockerTraceabilityPlugin}.
 * @author Oleg Nenashev
 */
public class DockerTraceabilityPluginConfiguration implements Describable<DockerTraceabilityPluginConfiguration> {
    
    private static final DockerTraceabilityPluginConfiguration DEFAULT = 
            new DockerTraceabilityPluginConfiguration(false);
            
    private final boolean createImageFingerprints;

    @DataBoundConstructor
    public DockerTraceabilityPluginConfiguration(boolean createImageFingerprints) {
        this.createImageFingerprints = createImageFingerprints;
    }
    
    @Override
    public Descriptor<DockerTraceabilityPluginConfiguration> getDescriptor() {
        return DESCRIPTOR;
    }

    public boolean isCreateImageFingerprints() {
        return createImageFingerprints;
    }
    
    /**
     * Gets the default configuration of {@link DockerTraceabilityPlugin}
     * @return Default configuration
     */
    public static final @Nonnull DockerTraceabilityPluginConfiguration getDefault() {
        return DEFAULT;
    }
    
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    
    public static class DescriptorImpl extends Descriptor<DockerTraceabilityPluginConfiguration> {

        @Override
        public String getDisplayName() {
            return "N/A";
        }
    }
}
