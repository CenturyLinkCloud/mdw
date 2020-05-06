package com.centurylink.mdw.cache.impl;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.AssetRef;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersion;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.asset.CommitInfo;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.util.AssetRefConverter;
import com.centurylink.mdw.util.file.VersionProperties;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.json.JSONException;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.ByteArrayInputStream;
import java.io.File;
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

    public static Asset getAsset(Long id) {
        AssetVersion assetVersion = getAssetVersions().get(id);
        if (assetVersion != null) {
            return loadAsset(assetVersion);
        }
        AssetRef ref = getAssetRef(id);
        if (ref != null) {
            // old-style asset ref
            return AssetRefConverter.getAsset(ref);
        }
        return null;
    }

    public static Asset getAsset(AssetVersionSpec spec) {
        AssetVersion assetVersion = getAssetVersion(spec);
        if (assetVersion != null) {
            return getAsset(assetVersion.getId());
        }
        AssetRef ref = getAssetRef(spec);
        if (ref != null) {
            // old-style asset ref
            return AssetRefConverter.getAsset(ref);
        }
        return null;
    }

    public static Process getProcess(AssetVersionSpec spec) {
        Asset asset = getAsset(spec);
        if (asset != null) {
            return getProcess(asset);
        }
        AssetRef ref = getAssetRef(spec);
        if (ref != null) {
            // old-style asset ref
            return AssetRefConverter.getProcess(ref);
        }
        return null;
    }

    public static Process getProcess(Long id) {
        Asset asset = getAsset(id);
        if (asset != null) {
            return getProcess(asset);
        }
        AssetRef ref = getAssetRef(id);
        if (ref != null) {
            // old-style asset ref
            return AssetRefConverter.getProcess(ref);
        }
        return null;
    }

    /**
     * Includes current version if committed to Git.
     */
    public static List<Process> getProcesses() {
        List<Process> processes = new ArrayList<>();
        for (AssetVersion assetVersion : getAssetVersions().values()) {
            if (assetVersion.getName().endsWith(".proc")) {
                Asset asset = loadAsset(assetVersion);
                if (asset != null) {
                    Process process = getProcess(asset);
                    if (process != null)
                        processes.add(process);
                }
            }
        }
        if (PropertyManager.getBooleanProperty(PropertyNames.MDW_ASSET_REF_ENABLED, false)) {
            // old-style asset ref
            for (AssetRef ref : AssetRefCache.getAllProcessRefs()) {
                Process process = AssetRefConverter.getProcess(ref);
                if (process != null && !processes.contains(process)) {
                    processes.add(process);
                }
            }
        }
        return processes;
    }

    private static Process getProcess(Asset asset) {
        try {
            Process process = Process.fromString(asset.getStringContent());
            process.setId(asset.getId());
            process.setName(asset.getName().substring(0, asset.getName().length() - 5));
            process.setLanguage(Asset.PROCESS);
            process.setVersion(asset.getVersion());
            process.setPackageName(asset.getPackageName());
            return process;
        }
        catch (JSONException | YAMLException ex) {
            logger.debug("Malformed process content in " + asset.getLabel() + ex.getMessage());
            return null;
        }
    }

    public static TaskTemplate getTaskTemplate(Long taskId) {
        Asset asset = getAsset(taskId);
        if (asset != null) {
            return getTaskTemplate(asset);
        }
        AssetRef ref = getAssetRef(taskId);
        if (ref != null) {
            // old-style asset ref
            return AssetRefConverter.getTaskTemplate(ref);
        }
        return null;
    }

    public static TaskTemplate getTaskTemplate(AssetVersionSpec spec) {
        Asset asset = getAsset(spec);
        if (asset != null) {
            return getTaskTemplate(asset);
        }
        AssetRef ref = getAssetRef(spec);
        if (ref != null) {
            // old-style asset ref
            return AssetRefConverter.getTaskTemplate(ref);
        }
        return null;
    }

    private static TaskTemplate getTaskTemplate(Asset asset) {
        TaskTemplate taskTemplate = new TaskTemplate(new JsonObject(asset.getStringContent()));
        taskTemplate.setTaskId(asset.getId());
        taskTemplate.setName(asset.getName());
        taskTemplate.setLanguage(Asset.TASK);
        taskTemplate.setVersion(asset.getVersion());
        taskTemplate.setPackageName(asset.getPackageName());
        return taskTemplate;
    }

    public static AssetVersion getAssetVersion(AssetVersionSpec spec) {
        for (AssetVersion assetVersion : getAssetVersions().values()) {
            if (assetVersion.getPath().equals(spec.getPath()) && assetVersion.meetsSpec(spec)) {
                return assetVersion;
            }
        }
        AssetRef ref = getAssetRef(spec);
        if (ref != null) {
            // old-style asset ref
            AssetVersion assetVersion = new AssetVersion(ref.getDefinitionId(), ref.getPath(), spec.getVersion());
            assetVersion.setRef(ref.getRef());
            return assetVersion;
        }
        return null;
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
        if (PropertyManager.getBooleanProperty(PropertyNames.MDW_ASSET_REF_ENABLED, false)) {
            // old-style asset ref
            for (AssetRef assetRef : AssetRefCache.getRefs(assetPath)) {
                AssetVersion version = new AssetVersion(assetRef.getDefinitionId(), assetPath, assetRef.getVersion());
                version.setRef(assetRef.getRef());
                if (!assetVersions.containsKey(assetRef.getDefinitionId()))
                    versions.add(version);
            }
        }
        return versions;
    }

    private static AssetRef getAssetRef(AssetVersionSpec spec) {
        if (PropertyManager.getBooleanProperty(PropertyNames.MDW_ASSET_REF_ENABLED, false)) {
            return AssetRefCache.getAssetRef(spec);
        }
        return null;
    }
    private static AssetRef getAssetRef(Long id) {
        if (PropertyManager.getBooleanProperty(PropertyNames.MDW_ASSET_REF_ENABLED, false)) {
            return AssetRefCache.getAssetRef(id);
        }
        return null;
    }

    /**
     * Populates assetVersions and idToVersions, but does not load content
     */
    public static synchronized Map<Long,AssetVersion> load() throws Exception {
        int defaultDays = ApplicationContext.isDevelopment() ? 0 : 365;
        int days = PropertyManager.getIntegerProperty(PropertyNames.MDW_ASSET_HISTORY_DAYS, defaultDays);
        Map<Long,AssetVersion> myAssetVersions = new ConcurrentHashMap<>();
        if (days > 0) {
            long before = System.currentTimeMillis();
            VersionControlGit vcGit = getVersionControl();
            Date cutoff = new Date(new Date().getTime() - TimeUnit.MILLISECONDS.convert(days, TimeUnit.DAYS));
            for (Package pkg : PackageCache.getPackages()) {
                String pkgPath = ApplicationContext.getAssetRoot() + "/" + pkg.getName().replace('.', '/');
                String verPropsGitPath = vcGit.getRelativePath(new File(pkgPath + "/" + PackageDir.VERSIONS_PATH).toPath());
                for (CommitInfo commit : vcGit.getCommits(verPropsGitPath)) {
                    // versions file should be committed with any asset change, so its commit history is compared to cutoff
                    if (commit.getDate().compareTo(cutoff) >= 0) {
                        byte[] verPropContents = vcGit.readFromCommit(commit.getCommit(), verPropsGitPath);
                        if (verPropContents == null) {
                            logger.debug("Version properties file " + verPropsGitPath + " not found for commit " + commit.getCommit());
                        }
                        else {
                            VersionProperties verProps = new VersionProperties(new ByteArrayInputStream(verPropContents));
                            for (String assetName : verProps.stringPropertyNames()) {
                                String assetPath = pkg.getName() + "/" + assetName;
                                String version = "0";
                                try {
                                    version = AssetVersion.formatVersion(verProps.getVersion(assetName));
                                } catch (NumberFormatException ex) {
                                    logger.debug("Error parsing version for " + assetPath + ": " + ex.getMessage());
                                }
                                Long id = vcGit.getId(new File(assetPath + " v" + version));
                                // later commits come first -- keep that if id is already present
                                if (!myAssetVersions.containsKey(id)) {
                                    AssetVersion assetVersion = new AssetVersion(id, assetPath, version);
                                    assetVersion.setRef(commit.getCommit());
                                    myAssetVersions.put(id, assetVersion);
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

    private static VersionControlGit getVersionControl() throws DataAccessException {
        return (VersionControlGit) DataAccess.getAssetVersionControl(ApplicationContext.getAssetRoot());
    }

    public static Asset loadAsset(AssetVersion assetVersion) throws CachingException {
        Asset asset = null;
        try {
            byte[] contentBytes = read(assetVersion);

            if (contentBytes != null && contentBytes.length > 0) {
                asset = new Asset();
                asset.setId(assetVersion.getId());
                asset.setName(assetVersion.getName());
                asset.setLanguage(Asset.getFormat(assetVersion.getName()));
                asset.setVersion(AssetVersion.parseVersion(assetVersion.getVersion()));
                asset.setPackageName(assetVersion.getPath().substring(0, assetVersion.getPath().length() - assetVersion.getName().length() - 1));
                asset.setLoadDate(new Date());
                // do not load jar assets into memory
                if (!Asset.excludedFromMemoryCache(asset.getName()))
                    asset.setRawContent(contentBytes);
            }
            else {
                logger.debug("Asset " + assetVersion + " not found in commit " + assetVersion.getRef());
            }

            return asset;
        }
        catch (Exception ex) {
            throw new CachingException("Error loading " + assetVersion, ex);
        }
    }

    private static byte[] read(AssetVersion assetVersion) throws Exception {
        VersionControlGit vc = getVersionControl();
        String gitPath = vc.getRelativePath(getPath(assetVersion.getPath()));
        return vc.readFromCommit(assetVersion.getRef(), gitPath);
    }

    private static Path getPath(String assetPath) {
        int lastSlash = assetPath.lastIndexOf('/');
        String pkg = assetPath.substring(0, lastSlash);
        String name = assetPath.substring(lastSlash + 1);
        return new File(ApplicationContext.getAssetRoot() + "/" + pkg.replace('.', '/') + "/" + name).toPath();
    }
}
