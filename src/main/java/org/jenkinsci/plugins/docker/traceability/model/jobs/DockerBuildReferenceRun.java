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

import hudson.model.AbstractBuild;
import hudson.model.Fingerprint;
import hudson.model.Result;
import hudson.model.Run;
import java.io.File;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.lazy.LazyBuildMixIn;

/**
 * References Docker items.
 * This reference allows to workaround {@link Fingerprint} retention policies.
 * @author Oleg Nenashev
 */
public class DockerBuildReferenceRun extends AbstractBuild<DockerBuildReferenceJob, DockerBuildReferenceRun> {
    
    private String itemId;
    private @CheckForNull String itemName;
    private Type itemType;
    private long dockerTimestamp;
    
    
    public DockerBuildReferenceRun(DockerBuildReferenceJob job) throws IOException  {
        super(job);
    }
    
    public DockerBuildReferenceRun(DockerBuildReferenceJob job, File buildDir) throws IOException  {
        super(job, buildDir);
    }
    
    /**
     * Sets data items and generates an appropriate description
     * @param id
     * @param name
     * @param type
     * @param timestamp
     * @throws IOException Description update error
     */
    void set(@Nonnull String id, @CheckForNull String name, @Nonnull Type type, long timestamp)     
            throws IOException {
        this.itemId = id;
        this.itemName = name;
        this.itemType = type;
        this.dockerTimestamp = timestamp;
        setDescription("for "+type.getDisplayName()+" "+ (name != null ? name : id));
    }

    public String getItemId() {
        return itemId;
    }

    public Type getItemType() {
        return itemType;
    }

    /**
     * Optional name of the docker item
     * @return Name of the docker item
     */
    public @CheckForNull String getItemName() {
        return itemName;
    }
 
    public long getDockerTimestamp() {
        return dockerTimestamp;
    }

    @Override
    public void run() {
        onStartBuilding();
        setResult(Result.SUCCESS);
        onEndBuilding();
    }

    /**
     * Type of the {@link DockerBuildReferenceRun}.
     */
    public static enum Type {
        
        CONTAINER("container"),
        IMAGE("image");
        
        private final String typeName;

        private Type(String typeName) {
            this.typeName = typeName;
        }
        
        /**
         * Display name of the {@link Type}.
         * @return Display name for Web interfaces.
         *      May become localizable later, defaults to {@link #getTypeName()}.
         */
        public @Nonnull String getDisplayName() {
            return getTypeName();
        }
        
        /**
         * Short unique id of the {@link Type}.
         * @return a non-localizable string, which is being used internally
         */
        public @Nonnull String getTypeName() {
            return typeName;
        }
    }
}
