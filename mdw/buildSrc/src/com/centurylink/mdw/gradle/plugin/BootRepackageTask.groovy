package com.centurylink.mdw.gradle.plugin

import java.util.jar.JarFile
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.internal.impldep.org.hamcrest.core.StringStartsWith

import groovy.io.FileType

public class BootRepackageTask extends DefaultTask {

    @TaskAction
    public void repackage() {
        Project project = getProject()
        project.getTasks().withType(Jar.class, new BootRepackageAction())
    }
    
    class BootRepackageAction implements Action<Jar> {

        BootRepackageAction() {
        }

        @Override
        public void execute(Jar jarTask) {
            Project project = getProject()
            project.getConfigurations().getByName(Dependency.ARCHIVES_CONFIGURATION).getAllArtifacts().files.each { f ->
                if (f.name.endsWith('.jar') && hasMdwWar(f)) {
                    repackageArchive(f, false)
                }
                else if (f.name.endsWith('.war') && hasMdwWar(f)) {
                    repackageArchive(f, true)
                }
            }
        }
        
        private boolean hasMdwWar(File archiveFile) {
            new JarFile(archiveFile).withCloseable { jarFile ->
                def found = jarFile.entries().find { entry ->
                  entry.name.matches('(BOOT-INF|WEB-INF)/lib/mdw-[\\d\\.]*(\\-SNAPSHOT)?\\.war')
                }
                return found != null
            }
        }
        
        private void repackageArchive(File archive, isWar) {
            
            Logger logger = getLogger()
            logger.lifecycle('Repackaging: ' + archive) 

            File tempDir = File.createTempDir('mdw', '.tmp')
            
            unjar(archive, tempDir)
            logger.lifecycle('Unjarred Boot: ' + tempDir)
            
            String mdwWar;
            if (isWar)
                mdwWar = new FileNameFinder().getFileNames(tempDir.toString(), 'WEB-INF/lib/mdw-*.war').get(0)
            else
                mdwWar = new FileNameFinder().getFileNames(tempDir.toString(), 'BOOT-INF/lib/mdw-*.war').get(0)
            logger.debug('MDW WAR: ' + mdwWar)
            File mdwWarFile = new File(mdwWar)
            
            if (isWar) {
                // WEB-INF/classes ==> WEB-INF/classes
                unjar(mdwWarFile, new File(tempDir.toString() + '/WEB-INF/classes'), ['WEB-INF/classes'], ['WEB-INF/classes/META-INF'])
                // / ==> /WEB-INF/classes/mdw (excluding WEB-INF, META-INF)
                unjar(mdwWarFile, new File(tempDir.toString() + '/WEB-INF/classes/mdw'), null, ['WEB-INF', 'META-INF'])
                // META-INF/mdw ==> META-INF/mdw
                unjar(mdwWarFile, new File(tempDir.toString() + '/META-INF/mdw'), ['META-INF/mdw'])
            }
            else {
                // WEB-INF/classes ==> BOOT-INF/classes
                unjar(mdwWarFile, new File(tempDir.toString() + '/BOOT-INF/classes'), ['WEB-INF/classes'], ['WEB-INF/classes/META-INF'])
                // / ==> BOOT-INF/classes/mdw (excluding WEB-INF, META-INF)
                unjar(mdwWarFile, new File(tempDir.toString() + '/BOOT-INF/classes/mdw'), null, ['WEB-INF', 'META-INF'])
                // META-INF/mdw ==> META-INF/mdw
                unjar(mdwWarFile, new File(tempDir.toString() + '/META-INF/mdw'), ['META-INF/mdw'])
            }
            
            mdwWarFile.delete()
            String archivePath = archive.toString()
            String origArchiveName = archivePath + '.orig2';
            File origArchive = new File(origArchiveName)
            if (origArchive.exists())
                origArchive.delete()
            archive.renameTo(origArchiveName)

            rejar(new File(archivePath), tempDir)
            
            tempDir.deleteDir()
            
        }
        
        private void unjar(File jar, File destDir) {
            unjar(jar, destDir, null)    
        }
        
        private void unjar(File jar, File destDir, List<String> includes) {
            unjar(jar, destDir, includes, null)    
        }

        /**
         * includes/excludes are simple prefixes 
         */
        private void unjar(File jar, File destDir, List<String> includes, List<String> excludes) {
            new JarFile(jar).withCloseable { jarFile ->
                jarFile.entries().findAll { entry ->
                    String includePath = null
                    if (includes != null) {
                        includePath = includes.find {
                            entry.name.startsWith(it)
                        }
                    }
                    boolean exclude = false
                    if (excludes != null) {
                        exclude = excludes.any {
                            entry.name.startsWith(it)
                        }
                    }
                        
                    if ((includes == null || includePath != null) && !exclude) {
                        String outPath = includePath == null ? entry.name : entry.name.substring(includePath.length())
                        File outFile = new File(destDir.absolutePath + '/' + outPath)
                        if (includePath != null) {
                            getLogger().debug('including: ' + outFile)
                        }
                        if (entry.directory) {
                            outFile.mkdirs()
                        }
                        else {
                            outFile.setBytes(jarFile.getInputStream(entry).bytes)
                        }
                    }
                }
            }
        }
        
        private void rejar(File jar, File fromDir) {
            
            byte[] buffer = new byte[16 * 1024]
            new ZipOutputStream(new FileOutputStream(jar)).withCloseable { zipOut ->
                // zipOut.setLevel(Deflater.NO_COMPRESSION)
                fromDir.eachFileRecurse(FileType.ANY) { file ->
                    String entryName = file.getPath().substring(fromDir.getPath().length() + 1).replace('\\', '/')
                    if (file.isDirectory())
                        entryName += '/'
                    ZipEntry ze = new ZipEntry(entryName)
                    if (file.isFile()) {
                        ze.setMethod(ZipOutputStream.STORED)
                        // calculate crc32
                        new Crc32OutputStream().withCloseable { crcOut ->
                            file.withInputStream { fileIn ->
                                int len
                                while ((len = fileIn.read(buffer)) > 0)
                                    crcOut.write(buffer, 0, len)
                            }
                            ze.setSize(crcOut.getSize())
                            ze.setCompressedSize(crcOut.getSize())
                            ze.setCrc(crcOut.getCrc())
                        }
                    }
                    zipOut.putNextEntry(ze)
                    if (file.isFile()) {
                        file.withInputStream { fileIn ->
                            int len
                            while ((len = fileIn.read(buffer)) > 0)
                                zipOut.write(buffer, 0, len)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * An {@code OutputStream} that provides a CRC-32 of the data that is written to it.
     */
    private static final class Crc32OutputStream extends OutputStream {
        
        private final CRC32 crc32 = new CRC32();

        private long size = 0;
        public long getSize() { return size; }
        
        @Override
        public void write(int b) throws IOException {
            this.crc32.update(b);
            size++;
        }

        @Override
        public void write(byte[] b) throws IOException {
            this.crc32.update(b);
            size += b.length;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            this.crc32.update(b, off, len);
            size += len;
        }

        private long getCrc() {
            return this.crc32.getValue();
        }

    }    
}
