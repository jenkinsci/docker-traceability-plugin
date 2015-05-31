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
import jenkins.model.lazy.LazyBuildMixIn;

/**
 * References Docker items.
 * This reference allows to workaround {@link Fingerprint} retention policies.
 * @author Oleg Nenashev
 */
public class DockerBuildReferenceRun extends AbstractBuild<DockerBuildReferenceJob, DockerBuildReferenceRun> {
    
    private String dockerId;
    private Type type;
    private long dockerTimestamp;
    
    public DockerBuildReferenceRun(DockerBuildReferenceJob job) throws IOException  {
        super(job);
    }
    
    public DockerBuildReferenceRun(DockerBuildReferenceJob job, File buildDir) throws IOException  {
        super(job, buildDir);
    }
    
    void set(String dockerId, Type type, long timestamp) {
        this.dockerId = dockerId;
        this.type = type;
        this.dockerTimestamp = timestamp;
    }

    public String getDockerId() {
        return dockerId;
    }

    public Type getType() {
        return type;
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
        CONTAINER,
        IMAGE;
    }
}
