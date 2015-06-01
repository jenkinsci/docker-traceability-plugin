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
package org.jenkinsci.plugins.docker.traceability;

import hudson.Plugin;
import hudson.model.Api;
import hudson.model.Descriptor;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.security.PermissionScope;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.jenkinsci.plugins.docker.traceability.model.jobs.DockerBuildReferenceFactory;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Provides an ability to trace server deployments via fingerprints.
 * @author Oleg Nenashev
 */
@ExportedBean
public class DockerTraceabilityPlugin extends Plugin {

    private final static Logger LOGGER = Logger.getLogger(DockerTraceabilityPlugin.class.getName());

    public static final PermissionGroup PERMISSIONS = new PermissionGroup(DockerTraceabilityPlugin.class, Messages._DockerDeployment_Permissions_Title());
    public static final Permission SUBMIT = new Permission(PERMISSIONS,"Submit", Messages._DockerDeployment_Permissions_Submit_Permission_Description(), Permission.UPDATE, PermissionScope.JENKINS);
    /**
     * Allows to retrieve details like full container/info dumps.
     * Interfaces are being managed by {@link Jenkins#READ} permission.
     */
    public static final Permission READ_DETAILS = new Permission(PERMISSIONS,"Read", Messages._DockerDeployment_Permissions_ReadDetails_Permission_Description(), Permission.READ, PermissionScope.JENKINS);
    
    /**
     * Allows to delete entries like container references.
     * @see DockerEventsAction#doRemoveContainer(org.kohsuke.stapler.StaplerRequest, org.kohsuke.stapler.StaplerResponse, java.lang.String)
     */
    public static final Permission DELETE = new Permission(PERMISSIONS,"Delete", Messages._DockerDeployment_Permissions_Delete_Permission_Description(), Jenkins.ADMINISTER, PermissionScope.JENKINS);
     
    private DockerTraceabilityPluginConfiguration configuration;

    /**
     * Retrieves the plugin instance.
     * @return {@link DockerTraceabilityPlugin}
     * @throws IllegalStateException the plugin has not been loaded yet
     */
    public static @Nonnull DockerTraceabilityPlugin getInstance() {
        Jenkins j = Jenkins.getInstance();
        DockerTraceabilityPlugin plugin = j != null ? j.getPlugin(DockerTraceabilityPlugin.class) : null;
        if (plugin == null) { // Fail horribly
            // TODO: throw a graceful error
            throw new IllegalStateException("Cannot get the plugin's instance. Jenkins or the plugin have not been initialized yet");
        }
        return plugin;
    }
    
    @Override
    protected void load() throws IOException {
        super.load();
        if (configuration == null) {     
            configuration = DockerTraceabilityPluginConfiguration.getDefault();        
            save();
        }
    }

    @Override
    public void configure(StaplerRequest req, JSONObject formData) throws IOException, ServletException, Descriptor.FormException {
        configuration = req.bindJSON(DockerTraceabilityPluginConfiguration.class, formData); 
        save();
    }
     
    public Api getApi() {
        return new Api(this);
    }   

    public @Nonnull DockerTraceabilityPluginConfiguration getConfiguration() {
        return configuration != null ? configuration : DockerTraceabilityPluginConfiguration.getDefault();
    }

    @Override 
    public void start() throws Exception {
        load();
    }
}
