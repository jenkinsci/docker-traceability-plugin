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
package org.jenkinsci.plugins.docker.traceability.model;

import hudson.model.Fingerprint;
import hudson.model.Fingerprint.BuildPtr;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.docker.commons.fingerprint.DockerFingerprints;
import org.jenkinsci.plugins.docker.traceability.api.DockerTraceabilityReport;
import org.jenkinsci.plugins.docker.traceability.core.DockerTraceabilityHelper;
import org.jenkinsci.plugins.docker.traceability.dockerjava.api.command.InspectContainerResponse;
import org.jenkinsci.plugins.docker.traceability.dockerjava.api.command.InspectContainerResponse.ContainerState;
import org.jenkinsci.plugins.docker.traceability.dockerjava.api.command.InspectImageResponse;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerContainerRecord;
import org.jenkinsci.plugins.docker.traceability.fingerprint.DockerDeploymentFacet;
import org.jenkinsci.plugins.docker.traceability.util.FingerprintsHelper;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Implementation of report for REST API.
 * This report exposes the data, for which Global READ permissions are enough.
 * @author Oleg Nenashev
 * @see DockerTraceabilityReport
 */
@ExportedBean
@Restricted(NoExternalUse.class)
public class DockerAPIReport {
    
    private final String lastUpdate;
    private final Container container;
    private final Image image;
    private final String environment;
    private final Host host;
    private final List<String> parents;

    private DockerAPIReport(String lastUpdate, Container container, Image image, String environment, 
            Host host, List<String> parents) {
        this.lastUpdate = lastUpdate;
        this.container = container;
        this.image = image;
        this.environment = environment;
        this.host = host;
        this.parents = parents;
    }
    
    @ExportedBean
    public abstract static class Item {
        private final @CheckForNull String id;
        private final @CheckForNull String name;
        private final @CheckForNull String created;
        private final @CheckForNull FingerprintRef fingerprint;

        public Item(String id, String name, String created, @CheckForNull Fingerprint fingerprint) {
            this.id = id;
            this.name = name;
            this.created = created;
            this.fingerprint = (fingerprint != null) ? new FingerprintRef(fingerprint) : null;
        }

        @Exported(visibility = 999)
        public String getId() {
            return id;
        }

        @Exported(visibility = 999)
        public String getName() {
            return name;
        }

        /**
         * Gets time and date when the image has been created. 
         * @return String in the following format: {@code yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'} (up to nanoseconds)
         */
        @Exported(visibility = 999)
        public String getCreated() {
            return created;
        }

        @Exported(visibility = 999)
        public FingerprintRef getFingerprint() {
            return fingerprint;
        }   
    }
    
    @ExportedBean 
    public static class BuildPtrRef {
        
        final @Nonnull BuildPtr ptr;

        public BuildPtrRef(@Nonnull BuildPtr ptr) {
            this.ptr = ptr;
        }
        
        @Exported(visibility = 999)
        public String getName() {
            return ptr.getName();
        }
        
        @Exported(visibility = 999)
        public int getNumber() {
            return ptr.getNumber();
        }
    }
    
    @ExportedBean 
    public static class FingerprintRef {
        
        final @Nonnull Fingerprint fingerprint;

        public FingerprintRef(@Nonnull Fingerprint fingerprint) {
            this.fingerprint = fingerprint;
        }
        
        @Exported(visibility = 999)
        public String getHashString() {
            return fingerprint.getHashString();
        }
        
        @Exported(visibility = 999)
        public BuildPtrRef getOriginal() {
            BuildPtr ptr = fingerprint.getOriginal();
            return (ptr != null) ? new BuildPtrRef(ptr) : null;
        }
        
        @Exported(visibility = 999)
        public Hashtable<String, RangeSet> getUsages() {
            Hashtable<String, RangeSet> res = new Hashtable<String, RangeSet>(fingerprint.getUsages().size());
            for (Map.Entry<String, Fingerprint.RangeSet> set : fingerprint.getUsages().entrySet()) {
                res.put(set.getKey(), new RangeSet(set.getValue()));
            }
            return res;
        }
        
        @Exported(visibility = 999)
        public String getFileName() {
            return fingerprint.getFileName();
        }
        
        @Exported(visibility = 999)
        public String getTimestamp() {
            return DockerTraceabilityHelper.formatDate(fingerprint.getTimestamp());
        }
    }
    
    @ExportedBean
    public static class Range {
        
        final int start;
        final int end;

        public Range(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Exported(visibility = 999)
        public int getEnd() {
            return end;
        }

        @Exported(visibility = 999)
        public int getStart() {
            return start;
        }
    }
    
    @ExportedBean
    public static class RangeSet {
        
        private final List<Range> ranges;

        public RangeSet(@Nonnull Range ... inputRanges) {
            ranges = new ArrayList<Range>(inputRanges.length);
            for (Range r : inputRanges) {
                ranges.add(r);
            }
        }
        
        public RangeSet(@Nonnull Fingerprint.RangeSet ref) {
            ranges = new ArrayList<Range>(ref.getRanges().size());
            for (Fingerprint.Range range : ref.getRanges()) {
                ranges.add(new Range(range.getStart(), range.getEnd()));
            }
        }

        @Exported(visibility = 999)
        public List<Range> getRanges() {
            return ranges;
        }     
    }
    
    @ExportedBean
    public static class State {
        private final String lastStatus;
        private final String startedAt;
        private final String finishedAt;
        private final boolean running;
        private final boolean paused;
        private final int pid;
        private final int exitCode;
        
        public State(String lastStatus, ContainerState state) {
            this.lastStatus = lastStatus;
            this.startedAt = state.getStartedAt();
            this.finishedAt = state.getFinishedAt();
            this.running = state.isRunning();
            this.paused = state.isPaused();
            this.pid = state.getPid();
            this.exitCode = state.getExitCode();
        }

        @Exported(visibility = 999)
        public String getFinishedAt() {
            return finishedAt;
        }

        @Exported(visibility = 999)
        public String getStartedAt() {
            return startedAt;
        }

        @Exported(visibility = 999)
        public String getLastStatus() {
            return lastStatus;
        }

        @Exported(visibility = 999)
        public boolean isRunning() {
            return running;
        }

        @Exported(visibility = 999)
        public boolean isPaused() {
            return paused;
        }

        @Exported(visibility = 999)
        public int getPid() {
            return pid;
        }

        @Exported(visibility = 999)
        public int getExitCode() {
            return exitCode;
        }   
    }
    
    @ExportedBean
    public static class Host {
        
        private final String id;
        private final String name;

        public Host(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Exported(visibility = 999)
        public String getName() {
            return name;
        }

        @Exported(visibility = 999)
        public String getId() {
            return id;
        }        
    }
    
    public static class Container extends Item {
        
        private final State state;

        public Container(String id, String name, String created, Fingerprint fingerprint, State state) {
            super(id, name, created, fingerprint);
            this.state = state;
        }       

        @Exported(visibility = 999)
        public State getState() {
            return state;
        }      
    }
    
    public static class Image extends Item {

        public Image(String id, String name, String created, Fingerprint fingerprint) {
            super(id, name, created, fingerprint);
        }
    }

    @Exported(visibility = 999)
    public String getLastUpdate() {
        return lastUpdate;
    }

    @Exported(visibility = 999)
    public Container getContainer() {
        return container;
    }

    @Exported(visibility = 999)
    public Image getImage() {
        return image;
    }

    @Exported(visibility = 999)
    public String getEnvironment() {
        return environment;
    }

    @Exported(visibility = 999)
    public Host getHost() {
        return host;
    }

    @Exported(visibility = 999)
    public List<String> getParents() {
        return parents;
    }
    
    /**
     * Creates a report for a container.
     * @param containerId Container id
     * @return Generated report. Null if the data cannot be retrieved.
     */
    public static @CheckForNull DockerAPIReport forContainer(@Nonnull String containerId) {
        Fingerprint containerFP = DockerTraceabilityHelper.of(containerId);
        if (containerFP == null) {
            return null;
        }
        DockerDeploymentFacet facet = FingerprintsHelper.getFacet(containerFP, 
                DockerDeploymentFacet.class);
        if (facet == null) {
            return null;
        }
        final String lastStatus = facet.getLastStatus();
        final DockerContainerRecord lastRecord = facet.getLatest();
        if (lastRecord == null) {
            return null;
        }
        final DockerTraceabilityReport report = lastRecord.getReport();
        final String imageId = report.getImageId();
        @CheckForNull Fingerprint imageFP = null;
        if (imageId != null) {
            try {
                imageFP = DockerFingerprints.of(imageId);
            } catch (IOException ex) {
                // Do nothing
            } 
        }
        
        final InspectImageResponse inspectImageResponse = report.getImage();
        final InspectContainerResponse inspectContainerResponse = report.getContainer();
        if (inspectContainerResponse == null) {
            return null;
        }
        
        final Image image = new Image(imageId, report.getImageName(), 
                (inspectImageResponse != null) ? inspectImageResponse.getCreated() : "N/A", imageFP);
        final State state = new State(lastStatus, inspectContainerResponse.getState());
        final Container container = new Container(inspectContainerResponse.getId(), inspectContainerResponse.getName(), 
                inspectContainerResponse.getCreated(), containerFP, state);
        final Host host = new Host(report.getHostInfo().getID(), report.getHostInfo().getName());
        final String environment = report.getEnvironment();
        final String lastUpdate =  DockerTraceabilityHelper.formatTime(report.getEvent().getTime());
        return new DockerAPIReport(lastUpdate, container, image, environment, host, report.getParents());
    }
}
