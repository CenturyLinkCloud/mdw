package com.centurylink.mdw.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zip implements Operation {

    private File srcDir;
    private File zipFile;

    public Zip(File srcDir, File zipFile) {
        this.srcDir = srcDir;
        this.zipFile = zipFile;
    }

    @Override
    public Operation run(ProgressMonitor... progressMonitors) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos)) {
            Path dirPath = srcDir.toPath();
            Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    zos.putNextEntry(new ZipEntry(dirPath.relativize(file).toString().replace('\\', '/')));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!dir.equals(dirPath)) {
                        zos.putNextEntry(new ZipEntry(dirPath.relativize(dir).toString().replace('\\', '/') + "/"));
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return this;
    }
}
