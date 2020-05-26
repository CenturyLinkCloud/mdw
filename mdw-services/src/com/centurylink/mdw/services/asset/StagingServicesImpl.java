package com.centurylink.mdw.services.asset;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.asset.AssetHistory;
import com.centurylink.mdw.cli.Import;
import com.centurylink.mdw.cli.Props;
import com.centurylink.mdw.cli.Vercheck;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.SystemMessages;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DbAccess;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.file.VersionProperties;
import com.centurylink.mdw.git.GitBranch;
import com.centurylink.mdw.git.GitDiffs.DiffType;
import com.centurylink.mdw.git.GitProgressMonitor;
import com.centurylink.mdw.git.VersionControlGit;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetPath;
import com.centurylink.mdw.model.asset.AssetVersion;
import com.centurylink.mdw.model.asset.api.AssetInfo;
import com.centurylink.mdw.model.asset.api.PackageList;
import com.centurylink.mdw.model.asset.api.StagingArea;
import com.centurylink.mdw.model.system.Bulletin;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.workflow.PackageMeta;
import com.centurylink.mdw.service.data.user.UserGroupCache;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.StagingServices;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StagingServicesImpl implements StagingServices {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static final int CACHE_TIMEOUT = 600000; // 10 minutes

    /**
     * Returns the main version control (ie: asset location).
     */
    private VersionControlGit getMainVersionControl() throws ServiceException {
        try {
            AssetServices assetServices = ServiceLocator.getAssetServices();
            return assetServices.getVersionControl();
        }
        catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
    }

    public File getStagingDir() {
        String tempDir = PropertyManager.getProperty(PropertyNames.MDW_TEMP_DIR);
        return new File(tempDir + "/git/staging");
    }

    public File getStagingDir(String cuid) {
        return new File(getStagingDir() + "/" + STAGING + ApplicationContext.getRuntimeEnvironment() + "_" + cuid);
    }

    /**
     * Cloned specifically for user staging.
     */
    public VersionControlGit getStagingVersionControl(String cuid) throws ServiceException {
        VersionControlGit stagingVersionControl = new VersionControlGit(true);
        String gitUrl = PropertyManager.getProperty(PropertyNames.MDW_GIT_REMOTE_URL);
        if (gitUrl == null)
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Missing configuration: " + PropertyNames.MDW_GIT_REMOTE_URL);
        String gitUser = PropertyManager.getProperty(PropertyNames.MDW_GIT_USER);
        if (gitUser == null)
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Missing configuration: " + PropertyNames.MDW_GIT_USER);
        String gitPassword = PropertyManager.getProperty(PropertyNames.MDW_GIT_PASSWORD);
        if (gitPassword == null)
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Missing configuration: " + PropertyNames.MDW_GIT_PASSWORD);

        try {
            stagingVersionControl.connect(gitUrl, gitUser, gitPassword, getStagingDir(cuid));
            return stagingVersionControl;
        }
        catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
    }

    public StagingArea getUserStagingArea(String cuid) throws ServiceException {
        StagingArea cachedStagingArea = getCachedStagingArea(cuid);
        if (cachedStagingArea != null)
            return cachedStagingArea;
        synchronized (inProgressPrepares) {
            if (inProgressPrepares.containsKey(cuid))
                return inProgressPrepares.get(cuid);
        }

        GitBranch stagingBranch = getStagingBranch(cuid, getMainVersionControl());
        if (stagingBranch == null)
            return null;
        User user = getUser(cuid);
        StagingArea userStagingArea = new StagingArea(user.getCuid(), user.getName());
        userStagingArea.setBranch(stagingBranch);
        return userStagingArea;
    }

    public List<StagingArea> getStagingAreas() {
        String prefix = STAGING + ApplicationContext.getRuntimeEnvironment() + "_";
        List<StagingArea> stagingAreas = new ArrayList<>();
        File stagingDir = getStagingDir();
        if (stagingDir != null) {
            File[] files = stagingDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory() && file.getName().startsWith(prefix)) {
                        String cuid = file.getName().substring(prefix.length());
                        User user = UserGroupCache.getUser(cuid);
                        if (user != null)
                            stagingAreas.add(new StagingArea(user.getCuid(), user.getName()));
                    }
                }
            }
        }
        return stagingAreas;
    }

    private GitBranch getStagingBranch(String cuid, VersionControlGit vcGit) throws ServiceException {
        long before = System.currentTimeMillis();
        try {
            List<GitBranch> branches = vcGit.getRemoteBranches();
            Optional<GitBranch> opt = branches.stream().filter(b -> {
                return b.getName().equals(STAGING + ApplicationContext.getRuntimeEnvironment() + "_" + cuid);
            }).findFirst();
            return opt.orElse(null);
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
        finally {
            if (logger.isDebugEnabled()) {
                logger.debug("getStagingBranch(" + cuid + ") finished in " + (System.currentTimeMillis() - before) + " ms");
            }
        }
    }

    private static final Map<String,StagingArea> inProgressPrepares = new ConcurrentHashMap<>();
    private static final Map<String,StagingArea> cachedStagingAreas = new ConcurrentHashMap<>();

    private StagingArea getCachedStagingArea(String cuid) {
        synchronized (cachedStagingAreas) {
            StagingArea cachedStagingArea = null;
            if (cachedStagingAreas.containsKey(cuid)) {
                cachedStagingArea = cachedStagingAreas.get(cuid);
                if (System.currentTimeMillis() - cachedStagingArea.getLoaded() > CACHE_TIMEOUT
                        || !getStagingDir(cuid).isDirectory()) {
                    cachedStagingAreas.remove(cuid);
                    return null;
                }
            }
            return cachedStagingArea;
        }
    }

    @Override
    public StagingArea prepareStagingArea(String cuid, GitProgressMonitor progressMonitor) throws ServiceException {
        User user = getUser(cuid);
        StagingArea cachedStagingArea = getCachedStagingArea(user.getCuid());
        if (cachedStagingArea != null)
            return cachedStagingArea;

        StagingArea userStagingArea = new StagingArea(user.getCuid(), user.getName());
        String stagingBranchName = STAGING + ApplicationContext.getRuntimeEnvironment() + "_" + cuid;
        userStagingArea.setBranch(new GitBranch(null, stagingBranchName));

        synchronized (inProgressPrepares) {
            if (inProgressPrepares.containsKey(user.getCuid())) {
                return inProgressPrepares.get(user.getCuid());
            }
            else {
                inProgressPrepares.put(user.getCuid(), userStagingArea);
            }
        }

        VersionControlGit stagingVc = getStagingVersionControl(cuid);
        try {
            if (stagingVc.localRepoExists()) {
                GitBranch stagingBranch = getStagingBranch(cuid, stagingVc);
                if (stagingBranch != null && stagingVc.getBranch().equals(stagingBranchName)) {
                    synchronized (inProgressPrepares) {
                        inProgressPrepares.remove(user.getCuid());
                    }
                    // return synchronously
                    long before = System.currentTimeMillis();
                    stagingVc.pull(stagingBranchName);
                    userStagingArea.setBranch(stagingBranch);
                    long elapsed = System.currentTimeMillis() - before;
                    logger.debug("Branch " + stagingBranchName + " pulled in " + elapsed + " ms");
                    synchronized (cachedStagingAreas) {
                        cachedStagingAreas.put(user.getCuid(), userStagingArea);
                    }
                    return userStagingArea;
                }
                new Thread(() -> {
                    try {
                        stagingVc.pull(stagingVc.getBranch(), progressMonitor);
                        if (stagingBranch == null) {
                            String id = stagingVc.createBranch(stagingBranchName, stagingVc.getBranch(), progressMonitor);
                            userStagingArea.setBranch(new GitBranch(id, stagingBranchName));
                        }
                        else {
                            userStagingArea.setBranch(stagingBranch);
                        }
                        if (!stagingVc.getBranch().equals(stagingBranchName))
                            stagingVc.checkout(stagingBranchName);
                        if (progressMonitor != null)
                            progressMonitor.done();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        if (progressMonitor != null)
                            progressMonitor.error(ex);
                    } finally {
                        synchronized (inProgressPrepares) {
                            inProgressPrepares.remove(user.getCuid());
                        }
                        synchronized (cachedStagingAreas) {
                            cachedStagingAreas.put(user.getCuid(), userStagingArea);
                        }
                    }
                }).start();
            } else {
                String mainBranch = getMainVersionControl().getBranch();
                new Thread(() -> {
                    try {
                        stagingVc.clone(mainBranch, progressMonitor);
                        GitBranch stagingBranch = getStagingBranch(cuid, stagingVc);
                        if (stagingBranch == null) {
                            String id = stagingVc.createBranch(stagingBranchName, mainBranch, progressMonitor);
                            userStagingArea.setBranch(new GitBranch(id, stagingBranchName));
                        }
                        else {
                            userStagingArea.setBranch(stagingBranch);
                        }
                        if (!stagingVc.getBranch().equals(stagingBranchName))
                            stagingVc.checkout(stagingBranchName);
                        if (progressMonitor != null)
                            progressMonitor.done();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        if (progressMonitor != null)
                            progressMonitor.error(ex);
                    } finally {
                        synchronized (inProgressPrepares) {
                            inProgressPrepares.remove(user.getCuid());
                        }
                        synchronized (cachedStagingAreas) {
                            cachedStagingAreas.put(user.getCuid(), userStagingArea);
                        }
                    }
                }).start();
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }

        return userStagingArea;
    }

    private User getUser(String cuid) throws ServiceException {
        User user = UserGroupCache.getUser(cuid);
        if (user == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "User not found: " + cuid);
        return user;
    }

    @Override
    public PackageList getPackages(String cuid, boolean withVcsInfo) throws ServiceException {
        getUser(cuid); // throws if not found
        AssetServices assetServices = getAssetServices(cuid);
        return assetServices.getPackages(withVcsInfo);
    }

    @Override
    public void createPackage(String cuid, String packageName) throws ServiceException {
        User stagingUser = getUser(cuid); // throws if not found

        // must not exist on regular branch
        if (ServiceLocator.getAssetServices().getPackage(packageName) != null)
            throw new ServiceException(ServiceException.CONFLICT, "Package already exists: " + packageName);

        AssetServices assetServices = getAssetServices(cuid);
        assetServices.createPackage(packageName);

        // commit the meta file
        try {
            VersionControlGit vcGit = getStagingVersionControl(cuid);
            String comment = "Package meta created on " + vcGit.getBranch() + " by " + stagingUser.getName();
            String pkgPath = getVcAssetPath() + "/" + packageName.replace('.', '/');
            List<String> commitPaths = new ArrayList<>();
            commitPaths.add(pkgPath + "/" + PackageMeta.PACKAGE_YAML_PATH);
            vcGit.add(commitPaths);
            vcGit.commit(commitPaths, comment);
            vcGit.push();
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            logger.error("Error committing: " + packageName, ex);
        }
    }

    @Override
    public void createAsset(String cuid, String assetPath, String template) throws ServiceException {
        User stagingUser = getUser(cuid);

        // must not exist on regular branch
        if (ServiceLocator.getAssetServices().getAsset(assetPath) != null)
            throw new ServiceException(ServiceException.CONFLICT, "Asset already exists: " + assetPath);

        AssetServices assetServices = getAssetServices(cuid);
        assetServices.createAsset(assetPath, template);

        logger.info("Asset created: " + assetPath);

        try {
            VersionControlGit vcGit = getStagingVersionControl(cuid);
            String comment = "Created on " + vcGit.getBranch() + " by " + stagingUser.getName();
            String pkgName = AssetServices.packageName(assetPath);
            String pkgPath = getVcAssetPath() + "/" + pkgName.replace('.', '/');
            List<String> commitPaths = new ArrayList<>();
            commitPaths.add(pkgPath + "/" + AssetServices.assetName(assetPath));
            commitPaths.add(pkgPath + "/" + PackageMeta.VERSIONS_PATH);
            vcGit.add(commitPaths);
            vcGit.commit(commitPaths, comment);
            vcGit.push();

            logger.info("Asset pushed: " + assetPath);
            stageAssets(cuid, Arrays.asList(assetPath));
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            logger.error("Error committing: " + assetPath, ex);
        }
    }

    @Override
    public AssetInfo getStagedAsset(String cuid, String assetPath) throws ServiceException {
        getUser(cuid); // throws if not found
        CommonDataAccess dataAccess = new CommonDataAccess();
        try {
            String value = dataAccess.getValue(OwnerType.USER, cuid, assetPath);
            if (!STAGED_ASSET.equals(value))
                return null;
            AssetInfo assetInfo = getAssetServices(cuid).getAsset(assetPath, true);
            if (assetInfo != null) {
                addDiffInfo(AssetServices.packageName(assetPath), assetInfo);
                return assetInfo;
            }
            else {
                return getGhostAsset(cuid, assetPath);
            }
        }
        catch (SQLException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
    }

    // TODO ghost assets for deleted
    @Override
    public SortedMap<String,List<AssetInfo>> getStagedAssets(String cuid) throws ServiceException {
        getUser(cuid); // throws if not found
        CommonDataAccess dataAccess = new CommonDataAccess();
        try {
            SortedMap<String,List<AssetInfo>> stagedAssets = new TreeMap<>();
            Map<String,String> userValues = dataAccess.getValues(OwnerType.USER, cuid);

            if (userValues != null) {
                AssetServices assetServices = getAssetServices(cuid);
                for (String userValueKey : userValues.keySet()) {
                    if (STAGED_ASSET.equals(userValues.get(userValueKey))) {
                        String userAsset = userValueKey;
                        String pkg = userAsset.substring(0, userAsset.lastIndexOf('/'));
                        try {
                            List<AssetInfo> pkgAssets = stagedAssets.computeIfAbsent(pkg, k -> new ArrayList<>());
                            AssetInfo assetInfo = assetServices.getAsset(userAsset, false);
                            if (assetInfo != null && assetInfo.getFile().exists()) {
                                addDiffInfo(pkg, assetInfo);
                                pkgAssets.add(assetInfo);
                            }
                            else {
                                pkgAssets.add(getGhostAsset(cuid, userAsset));
                            }
                        }
                        catch (ServiceException ex) {
                            if (ex.getCause() != null && ex.getCause() instanceof DataAccessException) {
                                logger.error(ex.getMessage(), ex);
                            }
                        }
                    }
                }
            }

            return stagedAssets;
        }
        catch (SQLException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
    }

    private AssetInfo getGhostAsset(String cuid, String assetPath) throws ServiceException {
        AssetInfo assetInfo = new AssetInfo(new AssetPath(assetPath).asset);
        assetInfo.setVcsDiffType(DiffType.MISSING);
        return assetInfo;
    }

    /**
     * Staged asset vs main.
     */
    private void addDiffInfo(String pkg, AssetInfo assetInfo) throws ServiceException {
        AssetInfo mainAssetInfo = ServiceLocator.getAssetServices().getAsset(pkg + "/" + assetInfo.getName());
        if (mainAssetInfo == null) {
            assetInfo.setVcsDiffType(DiffType.EXTRA);
        }
        else if (assetInfo.getVcsDiffType() != DiffType.MISSING) {
            try {
                if (!FileUtils.contentEqualsIgnoreEOL(assetInfo.getFile(), mainAssetInfo.getFile(), null)) {
                    assetInfo.setVcsDiffType(DiffType.DIFFERENT);
                }
            }
            catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void stageAssets(String cuid, List<String> assets) throws ServiceException {
        CommonDataAccess dataAccess = new CommonDataAccess();
        try {
            // first check availability
            for (String asset : assets) {
                List<String> ownerIds = dataAccess.getValueOwnerIds(OwnerType.USER, asset, STAGED_ASSET);
                if (ownerIds.size() > 1)
                    throw new SQLException("Inconsistent db state for asset: " + asset);
                String owner = ownerIds.isEmpty() ? null : ownerIds.get(0);
                if (owner != null)
                    throw new ServiceException(ServiceException.CONFLICT, "Asset " + asset + " already staged by " + owner);
            }
            // then stage assets
            for (String asset : assets) {
                dataAccess.setValue(OwnerType.USER, cuid, asset, STAGED_ASSET);
            }
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (SQLException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
    }

    @Override
    public void unStageAssets(String cuid, List<String> assets) throws ServiceException {
        CommonDataAccess dataAccess = new CommonDataAccess();
        try {
            VersionControlGit vcGit = getStagingVersionControl(cuid);
            List<String> commitPaths = new ArrayList<>();
            for (String asset : assets) {
                // revert to main version
                AssetInfo mainAsset = ServiceLocator.getAssetServices().getAsset(asset);
                AssetServices stagedAssetServices = getAssetServices(cuid);
                String pkgName = AssetServices.packageName(asset);
                String slashyPkg = pkgName.replace('.', '/');
                String pkgPath = getVcAssetPath() + "/" + slashyPkg;
                if (mainAsset != null) {
                    AssetInfo stagedAsset = stagedAssetServices.getAsset(asset);
                    File stagedAssetFile;
                    if (stagedAsset == null) {
                        // staged asset could be deleted
                        stagedAssetFile = new File(getStagingAssetsDir(cuid) + "/" + slashyPkg + "/" + mainAsset.getName());
                    }
                    else {
                        stagedAssetFile = stagedAsset.getFile();
                    }
                    Files.copy(mainAsset.getFile().toPath(), stagedAssetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    stagedAssetServices.updateAssetVersion(asset, mainAsset.getVersion());
                }
                else {
                    getAssetServices(cuid).deleteAsset(asset);
                    stagedAssetServices.removeAssetVersion(asset);
                }
                commitPaths.add(pkgPath + "/" + AssetServices.assetName(asset));
                commitPaths.add(pkgPath + "/" + PackageMeta.VERSIONS_PATH);
                dataAccess.deleteValue(OwnerType.USER, cuid, asset, STAGED_ASSET);
            }
            if (!commitPaths.isEmpty()) {
                vcGit.add(commitPaths);
                vcGit.commit("Assets unstaged on " + vcGit.getBranch() + " by " + cuid);
                vcGit.push();
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
    }

    @Override
    public void deleteAsset(String cuid, String assetPath) throws ServiceException {
        getAssetServices(cuid).removeAssetVersion(assetPath);
        String pkg = AssetServices.packageName(assetPath);
        String pkgPath = getVcAssetPath() + "/" + pkg.replace('.', '/');
        VersionControlGit vcGit = getStagingVersionControl(cuid);
        try {
            vcGit.rm(pkgPath + "/" + AssetServices.assetName(assetPath));
            vcGit.add(pkgPath + "/" + PackageMeta.VERSIONS_PATH);
            logger.info(assetPath + " deleted on " + vcGit.getBranch() + " by " + cuid);
            vcGit.commit(assetPath + " deleted on " + vcGit.getBranch() + " by " + cuid);
            vcGit.push();
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
    }

    @Override
    public void rollbackAssets(String cuid, Map<String,String> assetVersions) throws ServiceException {
        User stagingUser = getUser(cuid);

        // stage all assets first
        List<String> assets = new ArrayList<>();
        assets.addAll(assetVersions.keySet());
        stageAssets(cuid, assets);

        // save rolled-back versions
        List<Asset> rolledBackAssets = new ArrayList<>();
        try {
            for (String assetPath : assets) {
                String version = assetVersions.get(assetPath);
                Asset asset = AssetHistory.getAsset(assetPath, version);
                if (asset == null)
                   throw new ServiceException(ServiceException.NOT_FOUND, "Asset version not found: " + assetPath + " v" + version);
                rolledBackAssets.add(asset);
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }

        try {
            AssetServices assetServices = ServiceLocator.getAssetServices();
            VersionControlGit vcGit = getStagingVersionControl(cuid);
            for (Asset rolledBackAsset : rolledBackAssets) {
                // save asset and version file
                String pkgSlashyPath = rolledBackAsset.getPackageName().replace('.', '/');
                File stagingAssetsDir = getStagingAssetsDir(cuid);
                File stagedFile = new File(stagingAssetsDir + "/" + pkgSlashyPath + "/" + rolledBackAsset.getName());
                Files.write(stagedFile.toPath(), rolledBackAsset.getContent());
                AssetInfo currentAssetInfo = assetServices.getAsset(rolledBackAsset.getPackageName() + "/" + rolledBackAsset.getName());
                String currentVer = currentAssetInfo.getJson().getString("version");
                int incrementedVer = AssetVersion.parseVersion(currentVer) + 1;
                File versionsFile = new File(stagingAssetsDir + "/" + pkgSlashyPath + "/" + PackageMeta.VERSIONS_PATH);
                VersionProperties versionProps = new VersionProperties(versionsFile);
                versionProps.setProperty(rolledBackAsset.getName(), String.valueOf(incrementedVer));
                versionProps.save();

                // commit staged changes
                String comment = "Rolled back to version " + rolledBackAsset.getVersionString() + " by " + stagingUser.getName();
                List<String> commitPaths = new ArrayList<>();
                String pkgPath = getVcAssetPath() + "/" + pkgSlashyPath;
                commitPaths.add(pkgPath + "/" + rolledBackAsset.getName());
                commitPaths.add(pkgPath + "/" + PackageMeta.VERSIONS_PATH);
                vcGit.add(commitPaths);
                vcGit.commit(commitPaths, comment);
                vcGit.push();
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
    }

    @Override
    public void promoteAssets(String cuid, String comment) throws ServiceException {
        try {
            // merge main branch into staging
            VersionControlGit vcGit = getStagingVersionControl(cuid);
            GitBranch stagingBranch = getStagingBranch(cuid, vcGit);
            if (stagingBranch == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Staging branch not found for user: " + cuid);
            String mainBranch = PropertyManager.getProperty(PropertyNames.MDW_GIT_BRANCH);
            vcGit.merge(mainBranch, stagingBranch.getName());
            vcGit.push();

            // merge staging back into main
            vcGit.merge(stagingBranch.getName(), mainBranch);
            String assetLoc = ServiceLocator.getAssetServices().getAssetRoot().toString();
            // but first autofix version conflicts
            Props.init("mdw.yaml");
            Vercheck vercheck = new Vercheck();
            vercheck.setConfigLoc(PropertyManager.getConfigLocation());
            vercheck.setAssetLoc(assetLoc);
            vercheck.setGitRoot(vcGit.getLocalDir());
            vercheck.setDebug(true);
            vercheck.setFix(true);
            vercheck.run();
            // now commit and push on mainBranch
            vcGit.commit(comment);
            vcGit.push();
            // merge any version changes back to staging
            vcGit.merge(mainBranch, stagingBranch.getName());
            vcGit.commit(comment);
            vcGit.push();

            // now refresh main branch with changes
            VersionControlGit mainVc = (VersionControlGit) ServiceLocator.getAssetServices().getVersionControl();
            Bulletin bulletin = SystemMessages.bulletinOn("Asset import in progress...");
            try (DbAccess dbAccess = new DbAccess()) {
                Import importer = new Import(mainVc.getLocalDir(), mainVc, mainBranch, false, dbAccess.getConnection());
                importer.setAssetLoc(assetLoc);
                importer.setConfigLoc(PropertyManager.getConfigLocation());
                importer.setGitRoot(mainVc.getLocalDir());
                importer.importAssetsFromGit();
                SystemMessages.bulletinOff(bulletin, "Asset import completed");
                CacheRegistration.getInstance().refreshCaches();
            }
            finally {
                SystemMessages.bulletinOff(bulletin, "Asset import completed");
            }
            logger.info("Assets promoted on " + vcGit.getBranch() + " by " + cuid);

            // remove staged assets
            CommonDataAccess dataAccess = new CommonDataAccess();
            SortedMap<String,List<AssetInfo>> stagedAssets = getStagedAssets(cuid);
            for (String pkg : stagedAssets.keySet()) {
                for (AssetInfo assetInfo : stagedAssets.get(pkg)) {
                    String assetPath = pkg + "/" + assetInfo.getName();
                    dataAccess.deleteValue(OwnerType.USER, cuid, assetPath, STAGED_ASSET);
                }
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
        finally {
            synchronized (cachedStagingAreas) {
                cachedStagingAreas.remove(cuid);
            }
        }
    }

    public String getVcAssetPath() throws ServiceException {
        return getMainVersionControl().getRelativePath(ApplicationContext.getAssetRoot().toPath());
    }

    public AssetServices getAssetServices(String cuid) throws ServiceException {
        VersionControlGit vcGit = getStagingVersionControl(cuid);
        File stagingAssetDir = new File(vcGit.getLocalDir().toString() + '/' + getVcAssetPath());
        return new AssetServicesImpl(vcGit, stagingAssetDir);
    }

    public File getStagingAssetsDir(String cuid) throws ServiceException {
        return new File(getStagingDir(cuid).toString() + '/' + getVcAssetPath());
    }

}
