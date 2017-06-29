package com.centurylink.mdw.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.springframework.boot.gradle.repackage.ProjectLibraries

public class BootRepackageTask extends DefaultTask {

    @TaskAction
    public void repackage() {
        Project project = getProject();
        ProjectLibraries libraries = getLibraries();
        project.getTasks().withType(Jar.class, new BootRepackageAction(extension, libraries));
    }
    
    class BootRepackageAction implements Action<Jar> {
        private final ProjectLibraries libraries;

        BootRepackageAction(ProjectLibraries libraries) {
            this.libraries = libraries;
        }

        @Override
        public void execute(Jar jarTask) {
        }
    }
}
