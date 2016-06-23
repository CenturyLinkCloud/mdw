/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.asset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.w3c.dom.Element;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.Content;
import com.centurylink.mdw.common.utilities.DesignatedHostSslVerifier;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.timer.CodeTimer;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.AssetFile;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.value.asset.Asset;
import com.centurylink.mdw.model.value.asset.PackageAssets;
import com.centurylink.mdw.model.value.asset.PackageList;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;

public class AssetServicesImpl implements AssetServices {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private File assetRoot;
    public File getAssetRoot() { return assetRoot; }

    private File archiveDir;
    private File gitRoot;
    private String gitBranch;
    private String gitRemoteUrl;

    /**
     * Relative to gitRoot
     */
    private String assetPath;

    public AssetServicesImpl() {
        String assetLoc = PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION);
        if (assetLoc != null) {
            assetRoot = new File(assetLoc);
            archiveDir = new File(assetRoot + "/" + PackageDir.ARCHIVE_SUBDIR);
        }
    }


    private VersionControlGit versionControl;
    public VersionControl getVersionControl() throws IOException {
        if (versionControl == null)
            versionControl = getVersionControlGit();
        return versionControl;
    }

    public void clearVersionControl() {
        versionControl = null;
    }

    private VersionControlGit getVersionControlGit() throws IOException {
        VersionControlGit versionControlGit = null;
        gitRemoteUrl = PropertyManager.getProperty(PropertyNames.MDW_GIT_REMOTE_URL);
        if (gitRemoteUrl != null) {
            gitRoot = new File(PropertyManager.getProperty(PropertyNames.MDW_GIT_LOCAL_PATH));
            if (gitRoot != null) {
                logger.info("Git root path for Asset Services: " + gitRoot);
                gitBranch = PropertyManager.getProperty(PropertyNames.MDW_GIT_BRANCH);
                if (gitBranch == null) {
                    logger.warn("Asset Services do not include Git information since " + PropertyNames.MDW_GIT_BRANCH + " is not set");
                }
                else {
                    String user = PropertyManager.getProperty(PropertyNames.MDW_GIT_USER);
                    String password = PropertyManager.getProperty(PropertyNames.MDW_GIT_PASSWORD);
                    if (user == null || password == null) {
                        logger.warn("Asset Services do not include Git information since credentials are not specified.");
                    }
                    else {
                        String gitTrustedHost = PropertyManager.getProperty(PropertyNames.MDW_GIT_TRUSTED_HOST);
                        if (gitTrustedHost != null) {
                            try {
                                DesignatedHostSslVerifier.setupSslVerification(gitTrustedHost);
                            }
                            catch (Exception ex) {
                                throw new IOException(ex.getMessage(), ex);
                            }
                        }
                        versionControlGit = new VersionControlGit();
                        versionControlGit.connect(gitRemoteUrl, user, password, gitRoot);
                        assetPath = versionControlGit.getRelativePath(assetRoot);

                        if (!versionControlGit.localRepoExists()) {
                            // something's amiss -- startup should have cloned from Git
                            throw new IOException("Git root: " + gitRoot + " does not exist.");
                        }
                    }
                }
            }
        }
        return versionControlGit;
    }

    public byte[] getAssetContent(String assetPath) throws ServiceException {
            return RuleSetCache.getRuleSet(assetPath).getRuleSet().getBytes();
    }

    public void saveAsset(String asset, Object content, String user, String comment) throws ServiceException {
        if (asset == null)
            throw new ServiceException("Missing parameter: " + " asset");
        int slashIdx = asset.indexOf('/');
        if (slashIdx == -1)
            throw new ServiceException("Asset name must be qualified: MyPackage/MyText.txt");
        String pkg = asset.substring(0, slashIdx);
        String name = asset.substring(slashIdx + 1);
        int dotIdx = name.indexOf('.');
        if (dotIdx == -1)
            throw new ServiceException("Asset format must be identified by a filename extension.");
        String ext = name.substring(dotIdx);
        String language = RuleSetVO.getFormat(name);
        if (language == null)
            throw new ServiceException("Asset format unknown for extension: '" + ext + "'");
        boolean isBinary = RuleSetVO.isBinary(language);

        // determine content
        if (content == null)
            throw new ServiceException("Missing parameter: " + " Content");
        if (content instanceof Content) {
            Content contentXmlBean = (Content) content;
            if (contentXmlBean.getAny() == null || contentXmlBean.getAny().isEmpty()) {
                throw new ServiceException("Missing content");
            }
            content = ((Element)contentXmlBean.getAny().get(0)).getTextContent();
        }

        if (user == null)
            throw new ServiceException("Missing parameter: " + " user");

        try {
            UserVO userVO = ServiceLocator.getUserManager().getUser(user);

            if (userVO == null)
                throw new ServiceException("Unknown user: " + user);
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            RuleSetVO ruleSetVo = new RuleSetVO();
            ruleSetVo.setName(name);
            ruleSetVo.setLanguage(language);
            ruleSetVo.setId(0L);
            ruleSetVo.setCreateUser(user);
            ruleSetVo.setComment(comment);
            RuleSetVO existing = RuleSetCache.getRuleSet(asset);
            if (userVO != null && StringUtils.isNotBlank(userVO.getName()) && existing!=null && existing.getModifyingUser()!= null && !existing.getModifyingUser().equals(userVO.getName())) {
                throw new ServiceException("The selected Asset locked by :" + existing.getModifyingUser());
            }
            if (existing == null)
                ruleSetVo.setVersion(1);
            else
                ruleSetVo.setVersion(existing.getVersion() + 1);
            if (content instanceof byte[]) {
                if (isBinary)
                    ruleSetVo.setRuleSet(RuleSetVO.encode((byte[])content));
                else
                    ruleSetVo.setRuleSet(new String((byte[])content));
            }
            else {
                ruleSetVo.setRuleSet(content.toString());
            }

            taskMgr.saveAsset(pkg, ruleSetVo, user);
            new RuleSetCache().clearCache();
            new PackageVOCache().clearCache();
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }

        return;
    }

    /**
     * Returns all the assets for the specified package.
     * Works only for VCS assets.  Does not use the RuleSetCache.
     */
    public PackageAssets getAssets(String packageName) throws ServiceException {

        try {
            PackageDir pkgDir = getPackageDir(packageName);
            if (!pkgDir.isDirectory())
                return null;
            List<Asset> assets = new ArrayList<Asset>();
            for (File file : pkgDir.listFiles())
                if (file.isFile()) {
                    assets.add(new Asset(pkgDir.getAssetFile(file)));
            }

            PackageAssets pkgAssets = new PackageAssets(pkgDir);
            pkgAssets.setAssets(assets);

            CodeTimer timer = new CodeTimer("AssetServices", true);
            addVersionControlInfo(pkgAssets);
            pkgAssets.sort();
            timer.stopAndLogTiming("addVersionControlInfo(PackageAssets)");

            return pkgAssets;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public Map<String,List<Asset>> getAssetsOfType(String format) throws ServiceException {
        Map<String,List<Asset>> packageAssets = new HashMap<String,List<Asset>>();
        List<File> assetRoots = new ArrayList<File>();
        assetRoots.add(assetRoot);
        try {
            CodeTimer timer = new CodeTimer("AssetServices", true);
            List<PackageDir> pkgDirs = findPackageDirs(assetRoots);
            for (PackageDir pkgDir : pkgDirs) {
                List<Asset> assets = null;
                for (File file : pkgDir.listFiles())
                    if (file.isFile() && file.getName().endsWith("." + format)) {
                        if (assets == null)
                            assets = new ArrayList<Asset>();
                        assets.add(new Asset(pkgDir.getAssetFile(file)));
                }
                if (assets != null)
                    packageAssets.put(pkgDir.getPackageName(), assets);
            }
            timer.logTimingAndContinue("getAssetsOfType(" + format + ")");
            return packageAssets;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
        catch (IOException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public PackageList getPackages() throws ServiceException {
        List<File> assetRoots = new ArrayList<File>();
        assetRoots.add(assetRoot);
        try {
            File vcsRoot = getVersionControl() == null ? null : gitRoot;
            PackageList pkgList = new PackageList(ApplicationContext.getServerHostPort(), assetRoot, vcsRoot);
            CodeTimer timer = new CodeTimer("AssetServices", true);
            List<PackageDir> pkgDirs = findPackageDirs(assetRoots);
            timer.logTimingAndContinue("findPackageDirs()");
            pkgList.setPackageDirs(pkgDirs);
            addVersionControlInfo(pkgList);
            pkgList.sort();
            timer.stopAndLogTiming("addVersionControlInfo(PackageList)");
            return pkgList;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
        catch (IOException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public PackageDir getPackage(String name) throws ServiceException {
        try {
            return getPackageDir(name);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }
    /**
     * Finds the next level of sibling PackageDirs under a set of non-package dirs.
     */
    private List<PackageDir> findPackageDirs(List<File> dirs) throws DataAccessException {
        List<PackageDir> pkgSubDirs = new ArrayList<PackageDir>();
        List<File> allSubDirs = new ArrayList<File>();

        for (File dir : dirs) {
            for (File sub : dir.listFiles()) {
                if (sub.isDirectory() && !sub.equals(archiveDir)) {
                    if (new File(sub + "/.mdw").isDirectory()) {
                        VersionControl vc = DataAccess.getAssetVersionControl(assetRoot);
                        PackageDir pkgSubDir = new PackageDir(assetRoot, sub, vc);
                        pkgSubDir.parse();
                        pkgSubDirs.add(pkgSubDir);
                    }
                    allSubDirs.add(sub);
                }
            }
        }

        if (!allSubDirs.isEmpty())
            pkgSubDirs.addAll(findPackageDirs(allSubDirs));

        return pkgSubDirs;
    }

    private void addVersionControlInfo(PackageList pkgList) throws ServiceException {
        try {
            VersionControlGit versionControl = (VersionControlGit) getVersionControl();
            if (gitBranch != null)
                pkgList.setVcsBranch(gitBranch);
            if (gitRemoteUrl != null)
                pkgList.setVcsRemoteUrl(gitRemoteUrl);

            if (versionControl != null) {
                List<PackageDir> missingPkgDirs = new ArrayList<PackageDir>();
                List<String> diffPaths = new ArrayList<String>();
                List<DiffEntry> diffs = versionControl.getDiffs(assetPath, true);
                for (DiffEntry diff : diffs) {
                    if (diff.getChangeType() == ChangeType.ADD && diff.getNewPath().endsWith(".mdw/package.xml")) {
                        int trim = ".mdw/package.xml".length();
                        String pkgName = diff.getNewPath().substring(assetPath.length() + 1, diff.getNewPath().length() - trim - 1).replace('/', '.');
                        PackageDir pkgDir = new PackageDir(assetRoot, pkgName);
                        pkgDir.setVcsMissing(true);
                        missingPkgDirs.add(pkgDir);
                    }
                    else {
                        if (diff.getNewPath() != null && !diffPaths.contains(diff.getNewPath()))
                            diffPaths.add(diff.getNewPath());
                        else if (diff.getOldPath() != null && !diffPaths.contains(diff.getOldPath()))
                            diffPaths.add(diff.getOldPath());
                    }
                }
                for (PackageDir pkgDir : pkgList.getPackageDirs()) {
                    String pkgVcPath = versionControl.getRelativePath(pkgDir);
                    for (String diffPath : diffPaths) {
                        if (diffPath.startsWith(pkgVcPath)) {
                            pkgDir.setHasVcsDiffs(true);
                            break;
                        }
                    }
                }

                pkgList.getPackageDirs().addAll(missingPkgDirs);
            }
        }
        catch (Exception ex) {
            logger.severeException("Unable to retrieve Git information for asset packages", ex);
        }
    }

    private void addVersionControlInfo(PackageAssets pkgAssets) throws ServiceException {
        try {
            VersionControlGit versionControl = (VersionControlGit) getVersionControl();
            if (versionControl != null) {
                String pkgVcPath = versionControl.getRelativePath(pkgAssets.getPackageDir());
                List<DiffEntry> diffs = versionControl.getDiffs(pkgVcPath, true);
                List<Asset> missingAssets = new ArrayList<Asset>();
                for (DiffEntry diff : diffs) {
                    if (diff.getChangeType() == ChangeType.ADD) {
//                        Asset missingAsset = new Asset(new File(gitRoot + "/" + diff.getNewPath()));
//                        missingAsset.setVcsMissing(true);
//                        missingAssets.add(missingAsset);
                    }
                    else {
                        for (Asset asset : pkgAssets.getAssets()) {
                            String assetVcPath = versionControl.getRelativePath(asset.getFile());
                            if (assetVcPath.equals(diff.getNewPath()) || assetVcPath.equals(diff.getOldPath())) {
                                asset.setVcsDiffType(diff.getChangeType().toString());
                                pkgAssets.getPackageDir().setHasVcsDiffs(true);
                                break;
                            }
                        }
                    }
                }
                pkgAssets.getAssets().addAll(missingAssets);
            }
        }
        catch (Exception ex) {
            logger.severeException("Unable to retrieve Git information for asset packages", ex);
        }
    }

    public PackageDir getPackageDir(String name) throws DataAccessException {
        File dir = new File(assetRoot + "/" + name.replace('.', '/'));
        if (new File(dir + "/.mdw").isDirectory()) {
            VersionControl vc = DataAccess.getAssetVersionControl(assetRoot);
            PackageDir pkgDir = new PackageDir(assetRoot, dir, vc);
            pkgDir.parse();
            return pkgDir;
        }
        else {
            throw new DataAccessException("Missing package metadata directory under: " + dir);
        }
    }

    public Asset getAsset(String path) throws ServiceException {
        try {
            int lastSlash = path.lastIndexOf('/');
            String pkg = path.substring(0, lastSlash);
            String assetName = path.substring(lastSlash + 1);
            PackageDir pkgDir = getPackageDir(pkg);
            AssetFile assetFile = pkgDir.getAssetFile(new File(pkgDir + "/" + assetName));
            if (assetFile.isFile()) {
                Asset asset = new Asset(assetFile);
                addVersionControlInfo(asset);
                return asset;
            }
            else {
                return null;
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    private void addVersionControlInfo(Asset asset) throws ServiceException {
        try {
            VersionControlGit versionControl = (VersionControlGit) getVersionControl();
            if (versionControl != null) {
                String assetVcPath = versionControl.getRelativePath(asset.getFile());
                List<DiffEntry> diffs = versionControl.getDiffs(assetVcPath);
                if (!diffs.isEmpty()) {
                    // TODO possible for more than one?
                    DiffEntry diff = diffs.get(diffs.size() - 1);
                    asset.setVcsDiffType(diff.getChangeType().toString());
                    if (!asset.isBinary())
                        asset.setVcsDiffOutput(versionControl.getDiffOutput(diffs));
                }
            }
        }
        catch (Exception ex) {
            logger.severeException("Unable to retrieve Git information for asset packages", ex);
        }
    }

}
