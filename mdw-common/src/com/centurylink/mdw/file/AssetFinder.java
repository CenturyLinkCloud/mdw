package com.centurylink.mdw.file;

import com.centurylink.mdw.model.asset.AssetPath;
import com.centurylink.mdw.model.asset.AssetVersion;
import com.centurylink.mdw.model.workflow.PackageMeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class AssetFinder {

    private final String packageName;
    private final Path packagePath;
    private final MdwIgnore mdwIgnore;

    public AssetFinder(Path assetRoot, String packageName) throws IOException {
        this.packageName = packageName;
        File packageDir = new File(assetRoot + "/" + packageName.replace('.', '/'));
        this.packagePath = packageDir.toPath();
        this.mdwIgnore = new MdwIgnore(packageDir);
    }

    private VersionProperties versionProperties;
    private VersionProperties getVersionProperties() throws IOException {
        if (versionProperties == null)
            versionProperties = new VersionProperties(new File(packagePath + "/" + PackageMeta.VERSIONS_PATH));
        return versionProperties;
    }

    public Map<File,AssetVersion> findAssets() throws IOException {
        Map<File,AssetVersion> assets = new HashMap<>();
        File[] packageFiles = packagePath.toFile().listFiles();
        if (packageFiles == null)
            throw new IOException("Bad package path: " + packagePath);

        for (File file : packageFiles) {
            if (file.isFile() && !mdwIgnore.isIgnore(file)) {
                int version = getVersionProperties().getVersion(file.getName());
                assets.put(file, new AssetVersion(packageName + "/" + file.getName(), version));
            }
        }
        return assets;
    }

    public AssetVersion findAsset(String path) throws IOException {
        AssetPath assetPath = new AssetPath(path);
        File file = new File(packagePath.toFile() + "/" + assetPath.asset);
        if (file.isFile()) {
            int version = getVersionProperties().getVersion(file.getName());
            return new AssetVersion(packageName + "/" + file.getName(), version);
        }
        return null;
    }
}
