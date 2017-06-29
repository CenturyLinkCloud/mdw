package com.centurylink.mdw.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskDependency
import org.springframework.boot.gradle.repackage.RepackageTask

class BootPlugin implements Plugin<Project> {

    public static final String BOOT_REPACKAGE_TASK_NAME = "mdwBootRepackage";
    
    @Override
    public void apply(Project project) {
        System.out.println("HELLO");
        
        BootRepackageTask task = project.getTasks().create(BOOT_REPACKAGE_TASK_NAME,
                BootRepackageTask.class);

        task.setGroup(BasePlugin.BUILD_GROUP);
        Configuration runtimeConfiguration = project.getConfigurations()
                .getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME);
        TaskDependency runtimeProjectDependencyJarTasks = runtimeConfiguration
                .getTaskDependencyFromProjectDependency(true, JavaPlugin.JAR_TASK_NAME);
        task.dependsOn(
                project.getConfigurations().getByName(Dependency.ARCHIVES_CONFIGURATION)
                        .getAllArtifacts().getBuildDependencies(),
                runtimeProjectDependencyJarTasks);
        // registerOutput(project, task);
        ensureTaskRunsOnAssembly(project, task);
        // ensureMainClassHasBeenFound(project, task);
        
    }

    private void ensureTaskRunsOnAssembly(Project project, Task task) {
        project.getTasks().getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(task);
    }

}
