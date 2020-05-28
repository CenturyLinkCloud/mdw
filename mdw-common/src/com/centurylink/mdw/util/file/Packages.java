package com.centurylink.mdw.util.file;

import com.centurylink.mdw.model.workflow.PackageMeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated
 * use {@link com.centurylink.mdw.file.Packages}
 */
@Deprecated
public class Packages extends com.centurylink.mdw.file.Packages {
    @Deprecated
    public Packages(File assetRoot) throws IOException {
        super(assetRoot);
        List<File> packageDirs = new ArrayList<>();
        findAssetPackageDirs(getAssetRoot(), packageDirs);
        for (File packageDir : packageDirs) {
            String packageName = getAssetPath(packageDir).replace('/', '.').replace('\\', '.');
            put(packageName, packageDir);
        }
    }

    private void findAssetPackageDirs(File from, List<File> into) throws IOException {
        for (File file : from.listFiles()) {
            if (file.isDirectory() && !file.getName().equals(PackageMeta.META_DIR) && !file.getName().equals("Archive")) {
                File meta = new File(file + "/" + PackageMeta.META_DIR);
                if (meta.isDirectory() && hasPackage(meta)) {
                    into.add(file);
                }
                findAssetPackageDirs(file, into);
            }
        }
    }

    private boolean hasPackage(File metaDir) {
        return new File(metaDir + "/package.json").isFile() || new File(metaDir + "/package.yaml").isFile();
    }

    private String getRelativePath(File from, File to) {
        Path fromPath = Paths.get(from.getPath()).normalize().toAbsolutePath();
        Path toPath = Paths.get(to.getPath()).normalize().toAbsolutePath();
        return fromPath.relativize(toPath).toString().replace('\\', '/');
    }

    private String getAssetPath(File file) throws IOException {
        return getRelativePath(getAssetRoot(), file);
    }
}
