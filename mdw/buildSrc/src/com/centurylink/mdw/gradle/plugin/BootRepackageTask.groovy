package com.centurylink.mdw.gradle.plugin

import java.util.jar.JarFile

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.springframework.boot.gradle.repackage.ProjectLibraries

public class BootRepackageTask extends DefaultTask {

    @TaskAction
    public void repackage() {
        Project project = getProject()
        // ProjectLibraries libraries = getLibraries()
        project.getTasks().withType(Jar.class, new BootRepackageAction())
    }
    
    class BootRepackageAction implements Action<Jar> {
        private final ProjectLibraries libraries

        BootRepackageAction() {
        }

        @Override
        public void execute(Jar jarTask) {
            System.out.println("HELLO THERE")
            Project project = getProject()
            project.getConfigurations().getByName(Dependency.ARCHIVES_CONFIGURATION).getAllArtifacts().files.each { f ->
                System.out.println("F: " + f)
                if (f.name.endsWith('.jar'))
                    repackageJar(f)
                else if (f.name.endsWith('.war'))
                    repackageWar(f)
            }
        }
        
        private void repackageJar(File jar) {
            println 'repackage jar: ' + jar 
            
            
            
            File tempDir = File.createTempDir('mdw', '.tmp');
            // tempDir.deleteOnExit()
            
            unjar(jar, tempDir)
            println 'UNJARRED: ' + tempDir

        }
        
        private void repackageWar(File war) {
            println 'repackage war: ' + war
        }
        
        private void unjar(File jar, File destDir) {
            JarFile jarFile = new JarFile(jar)
            jarFile.entries().findAll { entry ->
                File outFile = new File(destDir.absolutePath + '/' + entry.name)
                if (entry.directory) {
                    outFile.mkdirs()
                }
                else {
                    outFile.setBytes(jarFile.getInputStream(entry).bytes)
                }
            }
            jarFile.close()
        }
    }
}
