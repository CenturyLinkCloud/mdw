package com.centurylink.mdw.file;

import com.centurylink.mdw.model.workflow.PackageMeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class Packages extends TreeMap<String,File> {

    public static final String MDW_BASE = "com.centurylink.mdw.base";
    public static final List<String> DEFAULT_BASE_PACKAGES = new ArrayList<>();
    static {
        DEFAULT_BASE_PACKAGES.add(MDW_BASE);
        DEFAULT_BASE_PACKAGES.add("com.centurylink.mdw.db");
        DEFAULT_BASE_PACKAGES.add("com.centurylink.mdw.kotlin");
        DEFAULT_BASE_PACKAGES.add("com.centurylink.mdw.node");
        DEFAULT_BASE_PACKAGES.add("com.centurylink.mdw.react");
        DEFAULT_BASE_PACKAGES.add("com.centurylink.mdw.task");
        DEFAULT_BASE_PACKAGES.add("com.centurylink.mdw.testing");
        DEFAULT_BASE_PACKAGES.add("com.centurylink.mdw.dashboard");
    }

    // TODO: use "provider" from PackageMeta
    public static boolean isMdwPackage(String packageName) {
        return packageName.startsWith("com.centurylink.mdw.") &&
                !packageName.startsWith("com.centurylink.mdw.demo") &&
                !packageName.startsWith("com.centurylink.mdw.oracle") &&
                !packageName.startsWith("com.centurylink.mdw.ibmmq") &&
                !packageName.startsWith("com.centurylink.mdw.tibco") &&
                !packageName.startsWith("com.centurylink.mdw.tests.ibmmq") &&
                !packageName.startsWith("com.centurylink.mdw.tests.tibco") &&
                !packageName.startsWith("com.centurylink.mdw.internal") &&
                !packageName.startsWith("com.centurylink.mdw.ignore") &&
                !packageName.startsWith("com.centurylink.mdw.node.node_modules") &&
                !packageName.startsWith("com.centurylink.mdw.authCTL") &&
                !packageName.startsWith("com.centurylink.mdw.emp") &&
                !packageName.startsWith("com.centurylink.mdw.statusmgr") &&
                !packageName.startsWith("com.centurylink.mdw.tests.statusmgr");
    }

    private File assetRoot;
    public File getAssetRoot() { return assetRoot; }

    public Packages(File assetRoot) throws IOException {
        if (!assetRoot.isDirectory())
            throw new IOException("Asset root is not a directory: " + assetRoot.getAbsolutePath());
        this.assetRoot = assetRoot;
        Map<Path,PackageMeta> packages = new PackageFinder(assetRoot.toPath()).findPackages();
        for (Path pkgPath : packages.keySet()) {
            PackageMeta pkgMeta = packages.get(pkgPath);
            put(pkgMeta.getName(), pkgPath.toFile());
        }
    }

    @SuppressWarnings("unused")
    public List<File> getPackageDirs() {
        List<File> dirs = new ArrayList<>(values());
        dirs.sort(Comparator.comparing(f -> f.getPath().toLowerCase()));
        return dirs;
    }

    public List<String> getPackageNames() {
         List<String> packageNames = new ArrayList<>(keySet());
         Collections.sort(packageNames);
         return packageNames;
    }

    public List<File> getAssetFiles(String packageName) throws IOException {
        List<File> assetFiles = new ArrayList<>();
        assetFiles.addAll(new AssetFinder(assetRoot.toPath(), packageName).findAssets().keySet());
        return assetFiles;
    }

    /**
     * Whether a (relative) file path is the result of asset compilation
     * (as determined based on asset package names).
     */
    @SuppressWarnings("unused")
    public boolean isAssetOutput(String path) {
        for (String pkg : getPackageNames()) {
            if (path.startsWith(pkg) || path.startsWith(pkg.replace('.', '/')))
                return true;
        }
        return false;
    }
}
