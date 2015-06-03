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

import java.io.IOException;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Stores tests for {@link DockerTraceabilityPlugin}.
 * @author Oleg Nenashev
 */
public class DockerTraceabilityPluginTest {
    
    @Rule
    public JenkinsRule j = new JenkinsRule();
    
    @Test
    public void testRoundTrip() throws Exception {
        final DockerTraceabilityPlugin plugin = DockerTraceabilityPlugin.getInstance();
        
        // Default value
        assertFalse(plugin.getConfiguration().isCreateImageFingerprints());
        
        // Round-trip with false/true
        DockerTraceabilityPluginConfiguration config1 = new DockerTraceabilityPluginConfiguration(false, true);
        testRoundtrip(config1);
        
        // Round-trip with true/false
        DockerTraceabilityPluginConfiguration config2 = new DockerTraceabilityPluginConfiguration(true, false);
        testRoundtrip(config2);
    }
    
    private void testRoundtrip(DockerTraceabilityPluginConfiguration config)throws IOException {
        final DockerTraceabilityPlugin plugin = DockerTraceabilityPlugin.getInstance();
        plugin.configure(config);
        assertEquals(config.isCreateImageFingerprints(), plugin.getConfiguration().isCreateImageFingerprints());
        assertEquals(config.isShowRootAction(), plugin.getConfiguration().isShowRootAction());
        plugin.load();
        assertEquals(config.isCreateImageFingerprints(), plugin.getConfiguration().isCreateImageFingerprints());
        assertEquals(config.isShowRootAction(), plugin.getConfiguration().isShowRootAction());
    }
    
    /**
     * Sets the plugin configuration.
     * @param configuration Configuration to be set
     * @throws IOException Save error
     */
    public static void configure(DockerTraceabilityPluginConfiguration configuration) 
            throws IOException {
        final DockerTraceabilityPlugin plugin = DockerTraceabilityPlugin.getInstance();
        plugin.configure(configuration);
    }
}
