package com.blogspot.jvalentino.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * <p>A basic gradle plugin.</p>
 * @author jvalentino2
 */
class SshDemoPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.task('ssh', type:SshTask)
    }
}
