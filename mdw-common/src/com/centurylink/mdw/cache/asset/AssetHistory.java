package com.centurylink.mdw.cache.asset;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.git.VersionControlGit;
import com.centurylink.mdw.model.asset.*;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.PackageMeta;
import com.centurylink.mdw.file.VersionProperties;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * AssetHistory does not load current versions unless they're committed to Git.
 */
public class AssetHistory implements CacheService {

    private static volatile Map<Long,AssetVersion> assetVersions;

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public void refreshCache() {
        synchronized(AssetHistory.class) {
            clearCache();
        }
    }

    @Override
    public void clearCache() {
        synchronized(AssetHistory.class) {
            assetVersions = null;
        }
    }

    private static Map<Long,AssetVersion> getAssetVersions() {
        Map<Long,AssetVersion> myAssetVersions = assetVersions;
        if (myAssetVersions == null) {
            try {
                myAssetVersions = load();
            }
            catch (Exception ex){
                throw new CachingException(ex.getMessage(), ex);
            }
        }
        return myAssetVersions;
    }

    public static Asset getAsset(Long id) throws IOException {
        AssetVersion assetVersion = getAssetVersions().get(id);
        if (assetVersion != null) {
            return loadAsset(assetVersion);
        }
        return null;
    }

    /**
     * Specific asset version.
     */
    public static Asset getAsset(String assetPath, String version) throws IOException {
        for (AssetVersion assetVersion : getAssetVersions(assetPath)) {
            if (assetVersion.getVersion().equals(version))
                return loadAsset(assetVersion);
        }
        return null;
    }

    /**
     * Returns (unloaded) assets.  Includes current version if committed to Git.
     */
    public static List<Asset> getAssets() {
        List<Asset> assets = new ArrayList<>();
        for (AssetVersion assetVersion : getAssetVersions().values()) {
            Asset asset = new Asset(ApplicationContext.getAssetRoot(), assetVersion);
            assets.add(asset);
        }
        return assets;
    }

    /**
     * Includes current version if committed to Git.
     */
    public static List<AssetVersion> getAssetVersions(String assetPath) {
        List<AssetVersion> versions = new ArrayList<>();
        for (AssetVersion assetVersion : getAssetVersions().values()) {
            if (assetVersion.getPath().equals(assetPath))
                versions.add(assetVersion);
        }
        return versions;
    }

    public static AssetVersion getAssetVersion(AssetVersionSpec spec) {
        for (AssetVersion assetVersion : getAssetVersions().values()) {
            if (spec.isMatch(assetVersion))
                return assetVersion;
        }
        return null;
    }

    /**
     * Populates assetVersions and idToVersions, but does not load content.
     * Because of git log ordering, assetVersions are sorted latest first.
     */
    public static synchronized Map<Long,AssetVersion> load() throws Exception {
        int defaultDays = ApplicationContext.isDevelopment() ? 0 : 365;
        int days = PropertyManager.getIntegerProperty(PropertyNames.MDW_ASSET_HISTORY_DAYS, defaultDays);
        Map<Long,AssetVersion> myAssetVersions = new ConcurrentHashMap<>();
        if (days > 0) {
            long before = System.currentTimeMillis();
            VersionControlGit vcGit = getVersionControl();
            File assetRoot = ApplicationContext.getAssetRoot();
            Date cutoff = new Date(new Date().getTime() - TimeUnit.MILLISECONDS.convert(days, TimeUnit.DAYS));
            for (Package pkg : PackageCache.getPackages()) {
                String pkgPath = assetRoot + "/" + pkg.getName().replace('.', '/');
                String verPropsGitPath = vcGit.getRelativePath(new File(pkgPath + "/" + PackageMeta.META_DIR + "/" + PackageMeta.VERSIONS).toPath());
                for (CommitInfo commit : vcGit.getCommits(verPropsGitPath)) {
                    // versions file should be committed with any asset change, so its commit history is compared to cutoff
                    if (commit.getDate().compareTo(cutoff) >= 0) {
                        byte[] verPropContents = vcGit.readFromCommit(commit.getCommit(), verPropsGitPath);
                        if (verPropContents == null) {
                            logger.debug("Version properties file " + verPropsGitPath + " not found for commit " + commit.getCommit());
                        }
                        else {
                            VersionProperties verProps = new VersionProperties(new ByteArrayInputStream(verPropContents));
                            List<String> commitPkgAssets = vcGit.getAssetsForCommit(commit.getCommit(), vcGit.getRelativePath(new File(pkgPath).toPath()));
                            for (String assetName : verProps.stringPropertyNames()) {
                                String assetPath = pkg.getName() + "/" + assetName;
                                if (commitPkgAssets.contains(assetName)) {
                                    String version = "0";
                                    try {
                                        version = AssetVersion.formatVersion(verProps.getVersion(assetName));
                                    } catch (NumberFormatException ex) {
                                        logger.debug("Error parsing version for " + assetPath + ": " + ex.getMessage());
                                    }
                                    AssetVersion assetVersion = new AssetVersion(assetPath, version);
                                    // later commits come first -- keep that if id is already present
                                    if (!myAssetVersions.containsKey(assetVersion.getId())) {
                                        assetVersion.setRef(commit.getCommit());
                                        myAssetVersions.put(assetVersion.getId(), assetVersion);
                                    }
                                }
                                else {
                                    logger.debug("Asset " + assetPath + " referenced in versions not found for commit " + commit.getCommit());
                                }
                            }
                        }
                    }
                    else {
                        break;  // no further back for this version file
                    }
                }
            }
            long ms = System.currentTimeMillis() - before;
            logger.info("Loaded " + days + " days of asset history in " + ms + " ms");
        }
        assetVersions = myAssetVersions;
        return myAssetVersions;
    }

    private static VersionControlGit getVersionControl() throws IOException {
        return DataAccess.getAssetVersionControl(ApplicationContext.getAssetRoot());
    }

    static Asset loadAsset(AssetVersion assetVersion) throws IOException {
            Asset asset;
            try {
                byte[] contentBytes = read(assetVersion);
                if (contentBytes == null) {
                    // should not happen because we checked in load() whether asset was found in commit
                    throw new IOException("Asset " + assetVersion + " not found for commit " + assetVersion.getRef());
                }
                String pkg = assetVersion.getPath().substring(0, assetVersion.getPath().length() - assetVersion.getName().length() - 1);
                String name = assetVersion.getName();
                int version = AssetVersion.parseVersion(assetVersion.getVersion());
                asset = new Asset(pkg, name, version, null);
                // do not load jar assets into memory
                if (!ContentTypes.isExcludedFromMemoryCache(asset.getExtension()))
                    asset.setContent(contentBytes);
                return asset;
            }
            catch (Exception ex) {
                throw new IOException("Error loading " + assetVersion, ex);
            }
    }

    private static byte[] read(AssetVersion assetVersion) throws Exception {
        VersionControlGit vc = getVersionControl();
        String gitPath = vc.getRelativePath(getPath(assetVersion.getPath()));
        return vc.readFromCommit(assetVersion.getRef(), gitPath);
    }

    private static Path getPath(String assetPath) {
        AssetPath ap = new AssetPath(assetPath);
        return new File(ApplicationContext.getAssetRoot() + "/" + ap.pkg.replace('.', '/') + "/" + ap.asset).toPath();
    }
}
