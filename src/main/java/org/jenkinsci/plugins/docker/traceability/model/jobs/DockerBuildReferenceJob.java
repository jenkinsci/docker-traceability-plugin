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
package org.jenkinsci.plugins.docker.traceability.model.jobs;

import hudson.BulkChange;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Items;
import hudson.model.Node;
import hudson.model.ResourceList;
import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.queue.CauseOfBlockage;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import java.io.File;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import jenkins.model.lazy.LazyBuildMixIn;
import org.acegisecurity.AccessDeniedException;

/**
 *
 * @author Oleg Nenashev
 */
public class DockerBuildReferenceJob extends AbstractProject<DockerBuildReferenceJob, DockerBuildReferenceRun>
        implements TopLevelItem {

    static final String JOB_NAME = "Docker_Traceability_Manager";
    static final String ROOT_DIR = "jobs/"+JOB_NAME;
    
    private final SortedMap<String, Integer> byDockerId = new TreeMap<String, Integer>();
    
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    
    public DockerBuildReferenceJob(@Nonnull Jenkins jenkins) {
        super(jenkins, JOB_NAME);
    }
    
    @Override
    public boolean isBuildable() {
        return false;
    }
    
    /**
     * Retrieves a {@link DockerBuildReferenceRun} for the specified ID and type.
     * If there is no existing reference, a new one will be created.
     * The method presumes that IDs are unique across all types
     * @param id Id of docker item (container, image, etc.)
     * @param name Optional name of the docker item
     * @param type Docker item type
     * @param timestamp Time of the request. This time will be assigned to
     *      the newly created {@link DockerBuildReferenceRun}.
     * @return {@link DockerBuildReferenceRun} for the specified id
     * @throws IOException {@link DockerBuildReferenceRun} save error
     */
    protected synchronized @Nonnull DockerBuildReferenceRun forDockerItem(@Nonnull String id,
            @CheckForNull String name,
            @Nonnull DockerBuildReferenceRun.Type type, long timestamp) throws IOException {
        final Integer runNumber = byDockerId.get(id);
        DockerBuildReferenceRun run = runNumber !=null ? getBuildByNumber(runNumber) : null;
        if (run == null) {       
            run = newBuild();
            BulkChange ch = new BulkChange(run);
            try {
                run.set(id, name, type, timestamp);
                run.run();
                byDockerId.put(name, run.getNumber());
            } finally {
                ch.commit();
            }
            save();
        }
        return run;
    }

    @Override
    public File getRootDir() {
        final Jenkins j = Jenkins.getInstance();
        if (j == null) {
            throw new IllegalStateException("Jenkins instance is not ready");
        }
        return new File(j.getRootDir(), ROOT_DIR);
    }
    
    @Override
    public synchronized void removeRun(@Nonnull DockerBuildReferenceRun run) {
        super.removeRun(run);
        byDockerId.remove(run.getItemId(), run);
    }
    
    /**
     * Loads the job from the disk.
     * @return Null if the job does not exist.
     * @throws IOException Loading error
     */
    static @CheckForNull DockerBuildReferenceJob loadJob() throws IOException {
        final Jenkins j = Jenkins.getInstance();
        if (j == null) {
            throw new IOException("Jenkins instance is not ready");
        }
        
        final File configFile = new File(j.getRootDir(), ROOT_DIR);
        if (configFile.exists()) {
            final Item item = Items.load(j, configFile);
            if (!(item instanceof DockerBuildReferenceJob)) {
                throw new IOException("Loaded wrong class: "+item.getClass()+" instead of "+DockerBuildReferenceJob.class);
            }
            DockerBuildReferenceJob job = (DockerBuildReferenceJob)item;
            job.onLoad(j, JOB_NAME);
            return job;
        } else { // create new item
            DockerBuildReferenceJob job = new DockerBuildReferenceJob(j);
            j.add(job, job.getName());
            job.save();         
            job.onCreatedFromScratch();
            return job;
        }     
    }

    public TopLevelItemDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean isBuildBlocked() {
        return false;
    }

    @Override
    public String getWhyBlocked() {
        return null;
    }

    @Override
    public CauseOfBlockage getCauseOfBlockage() {
        return null;
    }

    @Override
    public void checkAbortPermission() {
        throw new AccessDeniedException(DockerBuildReferenceRun.class + " cannot be aborted");
    }

    @Override
    public boolean hasAbortPermission() {
        return false;
    }

    @Override
    public Node getLastBuiltOn() {
        return Jenkins.getInstance();
    }

    @Override
    public DescribableList<Publisher, Descriptor<Publisher>> getPublishersList() {
        //TODO: prohivit updates?
        return new DescribableList<Publisher, Descriptor<Publisher>>(this);
    }

    @Override
    protected final  Class<DockerBuildReferenceRun> getBuildClass() {
        return DockerBuildReferenceRun.class;
    }

    @Override
    public boolean isFingerprintConfigured() {
        return true; // We reference fingerprints => they're being "produced" by the jov
    }
    
    public static class DescriptorImpl extends TopLevelItemDescriptor {

        @Override
        public String getDisplayName() {
            return JOB_NAME;
        }

        @Override
        public TopLevelItem newInstance(ItemGroup parent, String name) {
            if (parent instanceof Jenkins) {
                return new DockerBuildReferenceJob((Jenkins)parent);
            }
            throw new IllegalStateException(DockerBuildReferenceJob.class+" can be created for "+Jenkins.class+" only");
        }

        @Override
        public boolean isApplicable(Descriptor descriptor) {
            return false; // Nobody can create this item
        }
    }
    
    private class Builds extends LazyBuildMixIn<DockerBuildReferenceJob, DockerBuildReferenceRun> {

        @Override
        protected DockerBuildReferenceJob asJob() {
            return DockerBuildReferenceJob.this;
        }

        @Override
        protected Class<DockerBuildReferenceRun> getBuildClass() {
            return DockerBuildReferenceRun.class;
        }
    }
}
