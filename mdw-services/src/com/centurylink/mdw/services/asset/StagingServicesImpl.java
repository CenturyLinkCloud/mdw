package com.centurylink.mdw.services.asset;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.dataaccess.file.GitBranch;
import com.centurylink.mdw.dataaccess.file.GitDiffs.DiffType;
import com.centurylink.mdw.dataaccess.file.GitProgressMonitor;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.asset.StagingArea;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.service.data.user.UserGroupCache;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.StagingServices;
import com.centurylink.mdw.util.file.Packages;
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

    /**
     * Returns the main version control (ie: asset location).
     */
    private VersionControlGit getMainVersionControl() throws ServiceException {
        try {
            AssetServices assetServices = ServiceLocator.getAssetServices();
            return (VersionControlGit) assetServices.getVersionControl();
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
        VersionControlGit stagingVersionControl = new VersionControlGit();
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
    }

    private static final Map<String, StagingArea> inProgressPrepares = new ConcurrentHashMap<>();

    @Override
    public StagingArea prepareStagingArea(String cuid, GitProgressMonitor progressMonitor) throws ServiceException {
        User user = getUser(cuid);
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
                    return userStagingArea;
                }
                new Thread(() -> {
                    try {
                        stagingVc.pull(stagingVc.getBranch(), progressMonitor);
                        if (stagingBranch == null)
                            stagingVc.createBranch(stagingBranchName, stagingVc.getBranch(), progressMonitor);
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
                    }
                }).start();
            } else {
                String mainBranch = getMainVersionControl().getBranch();
                new Thread(() -> {
                    try {
                        stagingVc.clone(mainBranch, progressMonitor);
                        GitBranch stagingBranch = getStagingBranch(cuid, stagingVc);
                        if (stagingBranch == null)
                            stagingVc.createBranch(stagingBranchName, mainBranch, progressMonitor);
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
        AssetInfo assetInfo = new AssetInfo(getStagingDir(new File(cuid) + "/" + getVcAssetPath()), assetPath);
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
        else {
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
                String pkgPath = getVcAssetPath() + "/" + pkgName.replace('.', '/');
                if (mainAsset != null) {
                    AssetInfo stagedAsset = stagedAssetServices.getAsset(asset);
                    Files.copy(mainAsset.getFile().toPath(), stagedAsset.getFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
                    stagedAssetServices.updateAssetVersion(asset, mainAsset.getRevision().getVersion());
                }
                else {
                    getAssetServices(cuid).deleteAsset(asset);
                    stagedAssetServices.removeAssetVersion(asset);
                }
                commitPaths.add(pkgPath + "/" + AssetServices.assetName(asset));
                commitPaths.add(pkgPath + "/" + Packages.META_DIR + "/" + Packages.VERSIONS);
                dataAccess.deleteValue(OwnerType.USER, cuid, asset, STAGED_ASSET);
            }
            vcGit.add(commitPaths);
            vcGit.commit("Assets unstaged on " + vcGit.getBranch() + " by " + cuid);
            vcGit.push();
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
    }

    @Override
    public void deleteAsset(String cuid, String assetPath) throws ServiceException {
        AssetServices assetServices = getAssetServices(cuid);
        assetServices.deleteAsset(assetPath);
        assetServices.removeAssetVersion(assetPath);
        List<String> commitPaths = new ArrayList<>();
        String pkg = AssetServices.packageName(assetPath);
        String pkgPath = getVcAssetPath() + "/" + pkg.replace('.', '/');
        VersionControlGit vcGit = getStagingVersionControl(cuid);
        try {
            vcGit.rm(pkgPath + "/" + AssetServices.assetName(assetPath));
            vcGit.add(pkgPath + "/" + Packages.META_DIR + "/" + Packages.VERSIONS);
            vcGit.commit(assetPath + " deleted on " + vcGit.getBranch() + " by " + cuid);
            vcGit.push();
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
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
