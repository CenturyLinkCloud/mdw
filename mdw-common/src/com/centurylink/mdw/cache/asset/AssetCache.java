package com.centurylink.mdw.cache.asset;

import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.file.MdwIgnore;
import com.centurylink.mdw.file.VersionProperties;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersion;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.asset.ContentTypes;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.PackageMeta;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AssetCache implements CacheService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    // initially unpopulated
    private static Map<String,Asset> latestAssets;
    private static List<Asset> allAssets;
    private static Map<Long,Asset> allAssetsById;

    public void clearCache() {
        synchronized (AssetCache.class) {
            latestAssets = null;
            allAssets = null;
            allAssetsById = null;
        }
    }

    public synchronized void refreshCache() {
        clearCache();
    }

    public static synchronized boolean isLoaded() {
        return latestAssets != null;
    }

    public static Asset getAsset(String assetPath) throws IOException {
        getAssets(false); // make sure cache is loaded
        Asset asset = latestAssets.get(assetPath);
        if (asset != null) {
            if (!asset.isLoaded() && !ContentTypes.isExcludedFromMemoryCache(asset.getExtension())) {
                asset.load();
            }
            return asset;
        }
        return null;
    }

    public static Asset getAsset(String assetPath, int version) throws IOException {
        Asset latest = latestAssets.get(assetPath);
        if (latest != null) {
            // avoid iterating if possible
            if (version == 0 || latest.getVersion() == version) {
                if (!latest.isLoaded())
                    load(latest);
                return latest;
            }
        }
        for (Asset asset : getAssets(true)) {
            // even latest may be in archive if deleted
            if (asset.getPath().equals(assetPath)) {
                if (version == 0 || asset.getVersion() == version) {
                    if (!asset.isLoaded())
                        load(asset);
                    return asset;
                }
            }
        }
        return null;
    }

    public static Asset getAsset(Long id) throws IOException {
        // make sure assets are loaded
        getAssets(true);
        Asset asset = allAssetsById.get(id);
        if (asset != null && !asset.isLoaded()) {
            load(asset);
        }
        return asset;
    }

    private static void load(Asset asset) throws IOException {
        if (!ContentTypes.isExcludedFromMemoryCache(asset.getExtension())) {
            if (asset.isArchived()) {
                Asset loaded = AssetHistory.getAsset(asset.getId());
                if (loaded == null)
                    throw new IOException("Asset not found for id " + asset.getId() + " (" + asset.getLabel() + ")");
                asset.setContent(loaded.getContent());
            }
            else {
                asset.load();
            }
        }
    }

    public static Asset getJavaAsset(String className) throws IOException {
        return getCompilableAsset(className, "java");
    }

    /**
     * Asset that can be compiled into a JVM class.
     */
    public static Asset getCompilableAsset(String className, String ext) throws IOException {
        int lastDot = className.lastIndexOf('.');
        if (lastDot < 1 || lastDot > className.length() - 2)
            return null;  // unqualified name
        String pkg = className.substring(0, lastDot);
        String assetPath = pkg + "/" + className.substring(lastDot + 1) + "." + ext;
        return getAsset(assetPath);
    }

    /**
     * Either a specific version number can be specified, or a Smart Version can be specified
     * which designates an allowable range.  Relies on AssetHistory sorted by latest first.
     */
    public static Asset getAsset(AssetVersionSpec spec) throws IOException {
        for (Asset asset : getAssets(true)) {
            if (spec.isMatch(new AssetVersion(asset.getPath(), asset.getVersion()))) {
                if (!asset.isLoaded()) {
                    load(asset);
                }
                return asset;
            }
        }
        return null;
    }

    public static List<Asset> getAssets(String extension) {
        return getAssets(extension, false);
    }

    /**
     * Archived assets are potentially not loaded.  Retrieve by id to load.
     */
    public static List<Asset> getAssets(String extension, boolean withArchived) {
        List<Asset> assets = new ArrayList<>();
        for (Asset asset : getAssets(withArchived)) {
            if (asset.getName().endsWith("." + extension)) {
                if (!withArchived && !asset.isLoaded()) {
                    try {
                        load(asset);
                    } catch (IOException ex) {
                        logger.error("Error loading " + asset.getPath(), ex);
                    }
                }
                assets.add(asset);
            }
        }
        return assets;
    }

    public static List<Asset> getPackageAssets(String packageName) {
        return getAssets(false)
                .stream()
                .filter(asset -> asset.getPackageName().equals(packageName))
                .collect(Collectors.toList());
    }

    public static synchronized List<Asset> getAssets(boolean withArchived) {
        if (withArchived) {
            if (allAssets == null) {
                try {
                    allAssets = loadAllAssets();
                    allAssetsById = new ConcurrentHashMap<>();
                    for (Asset asset : allAssets) {
                        allAssetsById.put(asset.getId(), asset);
                    }
                }
                catch (IOException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
            return allAssets;
        }
        else {
            if (latestAssets == null) {
                try {
                    latestAssets = loadLatestAssets();
                }
                catch (IOException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
            return new ArrayList<>(latestAssets.values());
        }
    }

    /**
     * Includes archived.  Not inflated.
     */
    private static List<Asset> loadAllAssets() throws IOException {
        List<Asset> assets = findAssets();
        for (Asset asset : AssetHistory.getAssets()) {
            if (!assets.contains(asset)) {
                asset.setArchived(true);
                assets.add(asset);
            }
        }
        Collections.sort(assets);
        return assets;
    }

    private static Map<String,Asset> loadLatestAssets() throws IOException {
        Map<String,Asset> latestAssets = new ConcurrentHashMap<>();
        for (Asset asset: findAssets()) {
            latestAssets.put(asset.getPath(), asset);
        }
        return latestAssets;
    }

    private static List<Asset> findAssets() throws IOException {
        List<Asset> assets = new ArrayList<>();
        for (Package pkg : PackageCache.getPackages()) {
            File pkgDir = pkg.getDirectory();
            File verFile = new File(pkgDir + "/" + PackageMeta.META_DIR + "/" + PackageMeta.VERSIONS);
            VersionProperties verProps = new VersionProperties(verFile);
            MdwIgnore mdwIgnore = new MdwIgnore(pkgDir);
            for (File assetFile : pkgDir.listFiles()) {
                if (!mdwIgnore.isIgnore(assetFile)) {
                    int version = verProps.getVersion(assetFile.getName());
                    assets.add(new Asset(pkg.getName(), assetFile.getName(), version, assetFile));
                }
            }
        }
        Collections.sort(assets);
        return assets;
    }
}