/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.asset;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import com.centurylink.mdw.bpm.PackageDocument;
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
import com.centurylink.mdw.dataaccess.AssetRevision;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.AssetFile;
import com.centurylink.mdw.dataaccess.file.GitDiffs;
import com.centurylink.mdw.dataaccess.file.GitDiffs.DiffType;
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

    private VersionControl versionControl;
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
            String prop = PropertyManager.getProperty(PropertyNames.MDW_GIT_LOCAL_PATH);
            if (prop != null)
              gitRoot = new File(prop);
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
            if (pkgDir == null) {
                pkgDir = getGhostPackage(packageName);
                if (pkgDir == null)
                    throw new DataAccessException("Missing package metadata directory: " + pkgDir);
            }
            List<Asset> assets = new ArrayList<Asset>();
            if (!DiffType.MISSING.equals(pkgDir.getVcsDiffType())) {
                for (File file : pkgDir.listFiles())
                    if (file.isFile()) {
                        assets.add(new Asset(pkgDir.getAssetFile(file)));
                }
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
        catch (IOException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }
    /**
     * Finds the next level of sibling PackageDirs under a set of non-package dirs.
     */
    private List<PackageDir> findPackageDirs(List<File> dirs) throws IOException, DataAccessException {
        List<PackageDir> pkgSubDirs = new ArrayList<PackageDir>();
        List<File> allSubDirs = new ArrayList<File>();

        for (File dir : dirs) {
            for (File sub : dir.listFiles()) {
                if (sub.isDirectory() && !sub.equals(archiveDir)) {
                    if (new File(sub + "/.mdw").isDirectory()) {
                        PackageDir pkgSubDir = new PackageDir(assetRoot, sub, getAssetVersionControl());
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
            if (versionControl != null) {
                if (gitBranch != null)
                    pkgList.setVcsBranch(gitBranch);
                if (gitRemoteUrl != null)
                    pkgList.setVcsRemoteUrl(gitRemoteUrl);

                if (versionControl != null) {
                    GitDiffs diffs = versionControl.getDiffs(assetPath);

                    for (PackageDir pkgDir : pkgList.getPackageDirs()) {
                        String pkgVcPath = versionControl.getRelativePath(pkgDir);
                        // check for extra packages
                        for (String extraPath : diffs.getDiffs(DiffType.EXTRA)) {
                            if (extraPath.equals(pkgVcPath + "/" + PackageDir.PACKAGE_XML_PATH)) {
                                pkgDir.setVcsDiffType(DiffType.EXTRA);
                                break;
                            }
                        }
                        if (pkgDir.getVcsDiffType() == null) {
                            // check for packages with sub-content changes
                            for (DiffType diffType : DiffType.values()) {
                                if (pkgDir.getVcsDiffType() == null) { // not already found different
                                    for (String diff : diffs.getDiffs(diffType)) {
                                        if (diff.startsWith(pkgVcPath + "/") && !diff.startsWith(pkgVcPath + "/.mdw")) {
                                            pkgDir.setVcsDiffType(DiffType.DIFFERENT);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // check for missing packages
                    for (String missingDiff : diffs.getDiffs(DiffType.MISSING)) {
                        if (missingDiff.endsWith(PackageDir.PACKAGE_XML_PATH)) {
                            // add a ghost package
                            int trim = PackageDir.PACKAGE_XML_PATH.length();
                            String pkgName = missingDiff.substring(assetPath.length() + 1, missingDiff.length() - trim - 1).replace('/', '.');
                            PackageDir pkgDir = getGhostPackage(pkgName);
                            pkgList.getPackageDirs().add(pkgDir);
                        }
                    }
                }
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
                GitDiffs diffs = versionControl.getDiffs(pkgVcPath);
                pkgAssets.getPackageDir().setVcsDiffType(diffs.getDiffType(pkgVcPath + "/" + PackageDir.PACKAGE_XML_PATH));

                for (Asset asset : pkgAssets.getAssets()) {
                    String assetVcPath = versionControl.getRelativePath(asset.getFile());
                    DiffType diffType = diffs.getDiffType(assetVcPath);
                    if (diffType != null) {
                        asset.setVcsDiffType(diffType);
                        if (pkgAssets.getPackageDir().getVcsDiffType() == null)
                          pkgAssets.getPackageDir().setVcsDiffType(DiffType.DIFFERENT);
                    }
                }

                // check for missing assets
                for (String missingDiff : diffs.getDiffs(DiffType.MISSING)) {
                    if (missingDiff.startsWith(pkgVcPath + "/") && !missingDiff.startsWith(pkgVcPath + "/.mdw")) {
                        String assetName = missingDiff.substring(pkgVcPath.length() + 1);
                        pkgAssets.getAssets().add(getGhostAsset(pkgAssets.getPackageDir(), assetName));
                    }
                }
            }
        }
        catch (Exception ex) {
            logger.severeException("Unable to retrieve Git information for asset packages", ex);
        }
    }

    public PackageDir getPackageDir(String name) throws IOException, DataAccessException {
        File dir = new File(assetRoot + "/" + name.replace('.', '/'));
        if (new File(dir + "/.mdw").isDirectory()) {
            PackageDir pkgDir = new PackageDir(assetRoot, dir, getAssetVersionControl());
            pkgDir.parse();
            return pkgDir;
        }
        else {
            return null;
        }
    }

    /**
     * Falls back to DataAccess version control for asset versioning.
     */
    private VersionControl getAssetVersionControl() throws IOException, DataAccessException {
        VersionControl vc = getVersionControl();
        if (vc == null)
            vc = DataAccess.getAssetVersionControl(assetRoot);
        return vc;
    }

    public Asset getAsset(String path) throws ServiceException {
        try {
            int lastSlash = path.lastIndexOf('/');
            String pkgName = path.substring(0, lastSlash);
            String assetName = path.substring(lastSlash + 1);
            PackageDir pkgDir = getPackageDir(pkgName);
            if (pkgDir == null) {
                pkgDir = getGhostPackage(pkgName);
                if (pkgDir == null)
                    throw new DataAccessException("Missing package metadata directory: " + pkgDir);

                // ghost package contains ghost assets
                Asset asset = getGhostAsset(pkgDir, assetName);
                if (asset == null)
                    throw new DataAccessException("Missing asset file: " + path);
                return asset;
            }
            AssetFile assetFile = pkgDir.getAssetFile(new File(pkgDir + "/" + assetName));
            if (assetFile.isFile()) {
                Asset asset = new Asset(assetFile);
                addVersionControlInfo(asset);
                return asset;
            }
            else {
                Asset asset = getGhostAsset(pkgDir, assetName);
                if (asset == null)
                    throw new DataAccessException("Missing asset file for path: " + assetPath);
                return asset;
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
                GitDiffs diffs = versionControl.getDiffs(assetVcPath);
                asset.setVcsDiffType(diffs.getDiffType(assetVcPath));
            }
        }
        catch (Exception ex) {
            logger.severeException("Unable to retrieve Git information for asset packages", ex);
        }
    }

    private PackageDir getGhostPackage(String pkgName) throws Exception {
        PackageDir pkgDir = null;
        VersionControlGit gitVc = (VersionControlGit) getVersionControl();
        if (gitVc != null) {
            String pkgXmlPath = assetPath + "/" + pkgName + "/" + PackageDir.PACKAGE_XML_PATH;
            GitDiffs diffs = gitVc.getDiffs(pkgXmlPath);
            if (DiffType.MISSING.equals(diffs.getDiffType(pkgXmlPath))) {
                VersionControl vc = null;
                String versionFilePath = assetPath + "/" + pkgName + "/" + PackageDir.VERSIONS_PATH;
                String versionPropsStr = gitVc.getRemoteContentString(gitBranch, versionFilePath);
                if (versionPropsStr != null) {
                    Properties versionProps = new Properties();
                    versionProps.load(new ByteArrayInputStream(versionPropsStr.getBytes()));
                    vc = new GhostVersionControl(versionProps);
                }
                pkgDir = new PackageDir(assetRoot, pkgName, vc);
                pkgDir.setVcsDiffType(DiffType.MISSING);
                String pkgXml = ((VersionControlGit)getVersionControl()).getRemoteContentString(gitBranch, pkgXmlPath);
                PackageDocument pkgDoc = PackageDocument.Factory.parse(pkgXml);
                pkgDir.setPackageVersion(pkgDoc.getPackage().getVersion());
            }
            else {
                throw new IOException("Cannot locate missing package XML in version control: " + pkgXmlPath);
            }
        }
        return pkgDir;
    }

    private Asset getGhostAsset(PackageDir pkgDir, String assetName) throws Exception {
        Asset asset = null;
        VersionControlGit gitVc = (VersionControlGit) getVersionControl();
        if (gitVc != null) {
            String path = assetPath + "/" + pkgDir.getPackageName() + "/" + assetName;
            GitDiffs diffs = gitVc.getDiffs(path);
            if (DiffType.MISSING.equals(diffs.getDiffType(path))) {
                AssetFile assetFile = pkgDir.getAssetFile(new File(path));
                asset = new Asset(assetFile);
                asset.setVcsDiffType(DiffType.MISSING);
                if (pkgDir.getVcsDiffType() != DiffType.MISSING) {
                    // non-ghost pkg -- set version from remote
                    String versionFilePath = assetPath + "/" + pkgDir.getPackageName() + "/" + PackageDir.VERSIONS_PATH;
                    String versionPropsStr = gitVc.getRemoteContentString(gitBranch, versionFilePath);
                    if (versionPropsStr != null) {
                        Properties versionProps = new Properties();
                        versionProps.load(new ByteArrayInputStream(versionPropsStr.getBytes()));
                        String val = versionProps.getProperty(assetName);
                        if (val != null)
                            assetFile.setRevision(VersionControlGit.parseAssetRevision(val.trim()));
                    }
                }
            }
            else {
                throw new IOException("Cannot locate missing asset in version control: " + assetPath);
            }
        }
        return asset;
    }

    private class GhostVersionControl implements VersionControl {

        private Properties assetRevisions;

        GhostVersionControl(Properties assetRevisions) {
            this.assetRevisions = assetRevisions;
        }

        public AssetRevision getRevision(File file) throws IOException {
            if (assetRevisions == null)
                return null;
            String val = assetRevisions.getProperty(file.getName());
            if (val == null)
                return null;
            return VersionControlGit.parseAssetRevision(val.trim());
        }

        public void connect(String repositoryUrl, String user, String password, File localDir) throws IOException {
        }

        public long getId(File file) throws IOException {
            return 0;
        }

        public File getFile(long id) {
            return null;
        }

        public void setRevision(File file, AssetRevision rev) throws IOException {
        }

        public void clearId(File file) {
        }

        public void deleteRev(File file) throws IOException {
        }

        public void clear() {
        }
    }
}
