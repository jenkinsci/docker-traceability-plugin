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
            new DockerTraceabilityPluginConfiguration(false, false);
            
    private final boolean createImageFingerprints;
    
    private final boolean showRootAction;

    @DataBoundConstructor
    public DockerTraceabilityPluginConfiguration(boolean createImageFingerprints, 
            boolean showRootAction) {
        this.createImageFingerprints = createImageFingerprints;
        this.showRootAction = showRootAction;
    }
    
    @Override
    public Descriptor<DockerTraceabilityPluginConfiguration> getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Controls the behavior of {@link DockerTraceabilityPlugin} on missing parent images.
     * If enabled, the plugin will create missing image fingerprints for all
     * submitted reports, hence the plugin starts tracking containers being
     * created for images without parent image fingerprints.
     * @return true if {@link DockerTraceabilityPlugin} is allowed to create 
     *      image fingerprints on-demand. false by default
     */
    public boolean isCreateImageFingerprints() {
        return createImageFingerprints;
    }

    /**
     * Check if Jenkins should display the root action.
     * @return false by default
     */
    public boolean isShowRootAction() {
        return showRootAction;
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
