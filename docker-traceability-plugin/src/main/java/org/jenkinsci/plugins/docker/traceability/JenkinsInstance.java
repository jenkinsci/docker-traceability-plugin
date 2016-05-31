package org.jenkinsci.plugins.docker.traceability;

import javax.annotation.CheckForNull;

import jenkins.model.Jenkins;

public class JenkinsInstance {

    /**
     * Jenkins.getInstance() is annotated with CheckForNull since 1.565.1
     * CheckForNull was replaced with NonNull since 2.4
     * 
     * So, until the baseline is updated to 2.4, there is no way to make the build
     * pass for all baselines due to FindBugs errors (unless ignored), i.e:
     *      - Cannot remove the null check because it fails with baselines 1.565.1 - 2.3
     *      - Cannot use the null check because it fails with baselines 2.4++
     *
     * This methods wraps the call to Jenkins.getInstance() to force CheckForNull, so that the
     * same approach can be used with different versions.
     * 
     * TODO: Remove this class when baseline is bumped to 2.4 or higher.
     * 
     * @return a Jenkins instance
     */
    @CheckForNull
    public static Jenkins get() {
        return Jenkins.getInstance();
    }
}
