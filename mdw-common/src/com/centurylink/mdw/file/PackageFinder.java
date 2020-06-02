package com.centurylink.mdw.file;

import com.centurylink.mdw.model.Yamlable;
import com.centurylink.mdw.model.workflow.PackageMeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class PackageFinder {

    private final Path assetRoot;

    public PackageFinder(Path assetRoot) {
        this.assetRoot = assetRoot;
    }

    public PackageMeta findPackage(String name) throws IOException {
        File metaFile = new File(assetRoot + "/" + name.replace('.', '/') + "/" + PackageMeta.PACKAGE_YAML_PATH);
        if (!metaFile.isFile())
            return null;
        return parseMeta(metaFile);
    }

    public Map<Path,PackageMeta> findPackages() throws IOException {
        Map<Path,PackageMeta> packages = new HashMap<>();
        for (File metaFile : findMetaFiles()) {
            Path pkgPath = metaFile.getParentFile().getParentFile().toPath();
            try {
                PackageMeta pkgMeta = parseMeta(metaFile);
                packages.put(pkgPath, pkgMeta);
            } catch (Exception ex) {
                throw new IOException("Parsing failure: " + metaFile);
            }
        }
        return packages;
    }

    private PackageMeta parseMeta(File metaFile) throws IOException {
        String yaml = new String(Files.readAllBytes(metaFile.toPath()));
        PackageMeta pkgMeta = new PackageMeta(Yamlable.fromString(yaml));
        Path pkgPath = metaFile.getParentFile().getParentFile().toPath();
        Path relPath = assetRoot.normalize().relativize(pkgPath.normalize());
        String pkgName = relPath.toString().replace('\\', '.').replace('/', '.');
        if (!pkgMeta.getName().equals(pkgName))
            throw new IOException(pkgMeta.getName() + " does not match expected: " + pkgName);
        return pkgMeta;
    }

    public List<File> findMetaFiles() throws IOException {
        List<File> metaFiles = new ArrayList<>();
        Files.walkFileTree(assetRoot, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        if (new MdwIgnore(dir.toFile().getParentFile()).isIgnore(dir.toFile())) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        else {
                            return FileVisitResult.CONTINUE;
                        }
                    }
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                        File file = path.toFile();
                        if (file.isFile() && file.getName().equals(PackageMeta.PACKAGE_YAML)
                                && file.getParentFile().getName().equals(PackageMeta.META_DIR)
                                && file.getParentFile().getParent() != null) {
                            metaFiles.add(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
        return metaFiles;
    }
}
