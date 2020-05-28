/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.services.asset;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.app.Templates;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.git.GitDiffs;
import com.centurylink.mdw.git.GitDiffs.DiffType;
import com.centurylink.mdw.git.VersionControlGit;
import com.centurylink.mdw.discovery.GitDiscoverer;
import com.centurylink.mdw.discovery.GitHubDiscoverer;
import com.centurylink.mdw.discovery.GitLabDiscoverer;
import com.centurylink.mdw.file.AssetFinder;
import com.centurylink.mdw.file.PackageFinder;
import com.centurylink.mdw.model.Yamlable;
import com.centurylink.mdw.model.asset.AssetPath;
import com.centurylink.mdw.model.asset.AssetVersion;
import com.centurylink.mdw.model.asset.api.*;
import com.centurylink.mdw.model.system.MdwVersion;
import com.centurylink.mdw.model.workflow.PackageMeta;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.file.VersionProperties;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;

/**
 * Does not use AssetCache.
 */
public class AssetServicesImpl implements AssetServices {

    private static final StandardLogger logger = LoggerUtil.getStandardLogger();

    private final File assetRoot;
    public File getAssetRoot() {
        return assetRoot;
    }

    private static File gitRoot;
    private static File getGitRoot() {
        if (gitRoot == null) {
            String prop = PropertyManager.getProperty(PropertyNames.MDW_GIT_LOCAL_PATH);
            if (prop != null)
              gitRoot = new File(prop);
        }
        return gitRoot;
    }
    /**
     * from mdw.properties (not necessarily actual branch for switch scenario)
     */
    private static String getGitBranch() {
        return PropertyManager.getProperty(PropertyNames.MDW_GIT_BRANCH);
    }

    private static String getGitTag() {
        return PropertyManager.getProperty(PropertyNames.MDW_GIT_TAG);
    }

    private static String getGitRemoteUrl() {
        return PropertyManager.getProperty(PropertyNames.MDW_GIT_REMOTE_URL);
    }

    private static String getGitUser() {
        return PropertyManager.getProperty(PropertyNames.MDW_GIT_USER);
    }
    /**
     * Relative to gitRoot
     */
    private static String assetPath;
    private String getAssetPath() throws IOException {
        if (assetPath == null && getVersionControl() != null) {
            assetPath = getVersionControl().getRelativePath(assetRoot.toPath());
        }
        return assetPath;
    }

    public AssetServicesImpl() {
        assetRoot = ApplicationContext.getAssetRoot();
    }

    public AssetServicesImpl(VersionControlGit versionControl, File assetRoot) {
        this.versionControl = versionControl;
        gitRoot = versionControl.getLocalDir();
        this.assetRoot = assetRoot;
    }

    private VersionControlGit versionControl; // only set for non-standard vcs location
    public VersionControlGit getVersionControl() throws IOException  {
        if (versionControl == null) {
            return DataAccess.getAssetVersionControl(assetRoot);
        }
        else {
            return versionControl;
        }
    }

    @Override
    public PackageList getPackages(boolean withVcsInfo) throws ServiceException {
        try {
            PackageList pkgList = new PackageList(ApplicationContext.getServer(), assetRoot, getGitRoot());
            pkgList.setPackageInfos(findPackages());
            if (withVcsInfo)
                addVersionControlInfo(pkgList);
            pkgList.sort(); // in case ghost packages are added
            return pkgList;
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    @Override
    public PackageInfo getPackage(String name) throws ServiceException {
        try {
            PackageMeta pkgMeta = new PackageFinder(getAssetRoot().toPath()).findPackage(name);
            if (pkgMeta == null)
                return null;
            return new PackageInfo(pkgMeta.getName(), pkgMeta.getVersion().toString());
        }
        catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error in package: " + name, ex);
        }
    }

    private List<PackageInfo> findPackages() throws IOException {
        List<PackageInfo> packageInfos = new ArrayList<>();
        for (PackageMeta pkgMeta : new PackageFinder(getAssetRoot().toPath()).findPackages().values()) {
            packageInfos.add(new PackageInfo(pkgMeta.getName(), pkgMeta.getVersion().toString()));
        }
        Collections.sort(packageInfos);
        return packageInfos;
    }

    private PackageInfo getGhostPackage(String pkgName) throws Exception {
        PackageInfo pkgInfo = null;
        String pkgPath = pkgName.replace('.', '/');
        VersionControlGit gitVc = getVersionControl();
        if (gitVc != null && getGitUser() != null && getGitBranch() != null) {
            String pkgMetaFilePath = getAssetPath() + "/" + pkgPath + "/" + PackageMeta.PACKAGE_YAML_PATH;
            GitDiffs diffs = gitVc.getDiffs(getGitBranch(), pkgMetaFilePath);
            if (DiffType.MISSING.equals(diffs.getDiffType(pkgMetaFilePath))) {
                pkgInfo = new PackageInfo(pkgName);
                pkgInfo.setVcsDiffType(DiffType.MISSING);
                String metaContent = gitVc.getRemoteContentString(getGitBranch(), pkgMetaFilePath);
                if (metaContent != null) {
                    PackageMeta pkgMeta = new PackageMeta(Yamlable.fromString(metaContent));
                    pkgInfo.setVersion(pkgMeta.getVersion().toString());
                }
            }
            else {
                throw new FileNotFoundException("Cannot locate missing package meta in version control: " + pkgMetaFilePath);
            }
        }
        return pkgInfo;
    }

    @Override
    public PackageAssets getAssets(String packageName) throws ServiceException {
        return getAssets(packageName, false);
    }

    /**
     * Returns all the assets for the specified package.
     */
    @Override
    public PackageAssets getAssets(String packageName, boolean withVcsInfo) throws ServiceException {
        try {
            PackageInfo pkgInfo = getPackage(packageName);
            if (pkgInfo == null) {
                pkgInfo = getGhostPackage(packageName);
                if (pkgInfo == null)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Package not found: " + packageName);
            }
            List<AssetInfo> assets = new ArrayList<>();
            if (!DiffType.MISSING.equals(pkgInfo.getVcsDiffType())) {
                assets.addAll(findPackageAssets(packageName));
            }

            PackageAssets pkgAssets = new PackageAssets(pkgInfo.getName(), assets);

            if (withVcsInfo) {
                CodeTimer timer = new CodeTimer("AssetServices", true);
                addVersionControlInfo(pkgAssets);
                pkgAssets.sort();
                timer.stopAndLogTiming("addVersionControlInfo(PackageAssets)");
            }

            return pkgAssets;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public Map<String,List<AssetInfo>> getAssetsWithExtension(String extension) throws ServiceException {
        return findAssets(asset -> asset.getName().endsWith("." + extension));
    }

    @Override
    public Map<String,List<AssetInfo>> getAssetsWithExtensions(String[] extensions) throws ServiceException {
        List<String> exts = Arrays.asList(extensions);
        return findAssets(asset -> exts.contains(asset.getExtension()));
    }

    @Override
    public Map<String,List<AssetInfo>> findAssets(Predicate<AssetInfo> predicate) throws ServiceException {
        Map<String,List<AssetInfo>> packageAssets = new HashMap<>();
        try {
            for (PackageInfo pkg : findPackages()) {
                for (AssetInfo asset : findPackageAssets(pkg.getName())) {
                    if (predicate.test(asset)) {
                        List<AssetInfo> assets = packageAssets.get(pkg.getName());
                        if (assets == null) {
                            assets = new ArrayList<>();
                            packageAssets.put(pkg.getName(), assets);
                        }
                        assets.add(asset);
                    }
                }
            }
            return packageAssets;
        }
        catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    @Override
    public AssetPackageList getAssetPackageList(Query query) throws ServiceException {
        final List<String> exts = new ArrayList<>();
        String ext = query.getFilter("extension");
        if (ext != null) {
            if (ext.startsWith("[")) {
                exts.addAll(Arrays.asList(query.getArrayFilter("extension")));
            }
            else {
                exts.add(ext);
            }
        }
        Map<String,List<AssetInfo>> filteredAssets = findAssets(assetInfo ->
            exts.contains(assetInfo.getExtension())
        );

        List<PackageAssets> packageAssetList = new ArrayList<>();
        for (String pkg: filteredAssets.keySet()) {
            packageAssetList.add(new PackageAssets(pkg, filteredAssets.get(pkg)));
        }

        AssetPackageList assetPackageList = new AssetPackageList(packageAssetList);
        assetPackageList.setRetrieveDate(DatabaseAccess.getDbDate());
        assetPackageList.sort();
        return assetPackageList;
    }

    @Override
    public AssetInfo getAsset(String path) throws ServiceException {
        return getAsset(path, false);
    }

    @Override
    public AssetInfo getAsset(String path, boolean withVcsInfo) throws ServiceException {
        AssetPath assetPath = new AssetPath(path);
        PackageInfo pkgInfo = getPackage(assetPath.pkg);
        try {
            if (pkgInfo == null) {
                pkgInfo = getGhostPackage(assetPath.pkg);
                if (pkgInfo == null)
                    return null;
                // ghost package contains ghost assets
                return getGhostAsset(pkgInfo, assetPath);
            }
            AssetFinder finder = new AssetFinder(getAssetRoot().toPath(), assetPath.pkg);
            AssetVersion assetVersion = finder.findAsset(path);
            if (assetVersion == null) {
                return getGhostAsset(pkgInfo, assetPath);
            }
            File assetFile = new File(getAssetRoot() + "/" + assetPath.toPath());
            AssetInfo assetInfo = new AssetInfo(assetPath.asset, assetFile, assetVersion.getId(), assetVersion.getVersion());
            if (withVcsInfo)
                addVersionControlInfo(assetInfo);
            return assetInfo;
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Cannot retrieve " + path, ex);
        }
    }

    private List<AssetInfo> findPackageAssets(String packageName) throws IOException {
        List<AssetInfo> assets = new ArrayList<>();
        Map<File,AssetVersion> assetVers = new AssetFinder(getAssetRoot().toPath(), packageName).findAssets();
        for (File assetFile : assetVers.keySet()) {
            AssetVersion assetVer = assetVers.get(assetFile);
            assets.add(new AssetInfo(assetVer.getName(), assetFile, assetVer.getId(), assetVer.getVersion()));
        }
        return assets;
    }

    private void addVersionControlInfo(PackageList pkgList) {
        try {
            VersionControlGit versionControl = getVersionControl();
            if (versionControl != null) {
                if (getGitBranch() != null)
                    pkgList.setVcsBranch(getGitBranch());
                if (getGitTag() != null)
                    pkgList.setVcsTag(getGitTag());
                if (getGitRemoteUrl() != null)
                    pkgList.setVcsRemoteUrl(getGitRemoteUrl());

                pkgList.setGitBranch(versionControl.getBranch());

                if (!pkgList.getPackageInfos().isEmpty() && getGitUser() != null && getGitBranch() != null) {
                    GitDiffs diffs = versionControl.getDiffs(getGitBranch(), getAssetPath());

                    for (PackageInfo pkgInfo : pkgList.getPackageInfos()) {
                        String pkgPath = pkgInfo.getName().replace('.', '/');
                        String pkgVcPath = versionControl.getRelativePath(new File(getAssetRoot() + "/" + pkgPath).toPath());
                        // check for extra packages
                        for (String extraPath : diffs.getDiffs(DiffType.EXTRA)) {
                            if (extraPath.equals(pkgVcPath + "/" + PackageMeta.PACKAGE_YAML_PATH)) {
                                pkgInfo.setVcsDiffType(DiffType.EXTRA);
                                break;
                            }
                        }
                        if (pkgInfo.getVcsDiffType() == null) {
                            // check for packages with sub-content changes
                            for (DiffType diffType : DiffType.values()) {
                                if (pkgInfo.getVcsDiffType() == null) { // not already found different
                                    for (String diff : diffs.getDiffs(diffType)) {
                                        if (diff.startsWith(pkgVcPath + "/") && !diff.startsWith(pkgVcPath + "/.mdw")) {
                                            pkgInfo.setVcsDiffType(DiffType.DIFFERENT);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // check for missing packages
                    for (String missingDiff : diffs.getDiffs(DiffType.MISSING)) {
                        if (missingDiff.endsWith(PackageMeta.PACKAGE_YAML_PATH)) {
                            int trim = PackageMeta.PACKAGE_YAML_PATH.length();
                            // add a ghost package
                            String pkgName = missingDiff.substring(getAssetPath().length() + 1, missingDiff.length() - trim - 1).replace('/', '.');
                            PackageInfo pkgInfo = getGhostPackage(pkgName);
                            if (pkgInfo != null) {
                                pkgList.getPackageInfos().add(pkgInfo);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            logger.error("Unable to retrieve Git information for asset packages", ex);
        }
    }

    private void addVersionControlInfo(PackageAssets pkgAssets) {
        try {
            VersionControlGit versionControl = getVersionControl();
            if (versionControl != null && getGitUser() != null && getGitBranch() != null) {
                String pkg = pkgAssets.getPackageName();
                File pkgDir = new File(getAssetRoot() + "/" + pkg.replace('.', '/'));
                String pkgVcPath = versionControl.getRelativePath(pkgDir.toPath());
                GitDiffs diffs = versionControl.getDiffs(getGitBranch(), pkgVcPath);
                DiffType pkgDiffType = diffs.getDiffType(pkgVcPath + "/" + PackageMeta.PACKAGE_YAML_PATH);

                for (AssetInfo asset : pkgAssets.getAssets()) {
                    String assetVcPath = versionControl.getRelativePath(asset.getFile().toPath());
                    DiffType diffType = diffs.getDiffType(assetVcPath);
                    if (diffType != null) {
                        asset.setVcsDiffType(diffType);
                        if (pkgDiffType == null)
                            pkgDiffType = DiffType.DIFFERENT;
                    }
                }

                // check for missing assets
                for (String missingDiff : diffs.getDiffs(DiffType.MISSING)) {
                    if (missingDiff.startsWith(pkgVcPath + "/") && !missingDiff.startsWith(pkgVcPath + "/" + PackageMeta.META_DIR)) {
                        PackageInfo pkgInfo = new PackageInfo(pkg);
                        pkgInfo.setVcsDiffType(pkgDiffType);
                        AssetInfo asset = getGhostAsset(pkgInfo, new AssetPath(pkg + "/" + missingDiff.substring(pkgVcPath.length() + 1)));
                        if (asset != null)
                            pkgAssets.getAssets().add(asset);
                    }
                }
            }
        }
        catch (Exception ex) {
            logger.error("Unable to retrieve Git information for asset packages", ex);
        }
    }

    private void addVersionControlInfo(AssetInfo asset) {
        CodeTimer timer = new CodeTimer("addVersionControlInfo(AssetInfo)", true);
        try {
            VersionControlGit versionControl = getVersionControl();
            if (versionControl != null && getGitUser() != null && getGitBranch() != null) {
                String assetVcPath = versionControl.getRelativePath(asset.getFile().toPath());
                asset.setCommitInfo(versionControl.getCommitInfo(assetVcPath));
                GitDiffs diffs = versionControl.getDiffs(getGitBranch(), assetVcPath);
                asset.setVcsDiffType(diffs.getDiffType(assetVcPath));
            }
        }
        catch (Exception ex) {
            logger.error("Unable to retrieve Git information for asset packages", ex);
        }
        finally {
            timer.stopAndLogTiming(null);
        }
    }

    private AssetInfo getGhostAsset(PackageInfo pkgInfo, AssetPath assetPath) throws Exception {
        AssetInfo asset = null;
        VersionControlGit gitVc = getVersionControl();
        if (gitVc != null && getGitUser() != null && getGitBranch() != null) {
            String path = getAssetPath() + "/" + assetPath.toPath();
            GitDiffs diffs = gitVc.getDiffs(getGitBranch(), path);
            if (DiffType.MISSING.equals(diffs.getDiffType(path))) {
                asset = new AssetInfo(assetPath.asset);
                asset.setVcsDiffType(DiffType.MISSING);
                if (pkgInfo.getVcsDiffType() != DiffType.MISSING) {
                    // non-ghost pkg -- set version from remote
                    String versionFilePath = getAssetPath() + "/" + pkgInfo.getName() + "/" + PackageMeta.VERSIONS_PATH;
                    String versionPropsStr = gitVc.getRemoteContentString(getGitBranch(), versionFilePath);
                    if (versionPropsStr != null) {
                        VersionProperties versionProps = new VersionProperties(new ByteArrayInputStream(versionPropsStr.getBytes()));
                        int ver = versionProps.getVersion(assetPath.asset);
                        asset.setVersion(AssetVersion.formatVersion(ver));
                    }
                }
            }
            else {
                logger.warn("Cannot locate missing asset in version control: " + assetPath);
            }
        }
        return asset;
    }



    @Override
    public void createPackage(String packageName) throws ServiceException {
        if (getPackage(packageName) != null)
            throw new ServiceException(ServiceException.CONFLICT, "Package already exists: " + packageName);

        File dir = new File(assetRoot + "/" + packageName.replace('.', '/'));
        File metaDir = new File(dir + "/" + PackageMeta.META_DIR);
        if (metaDir.exists())
            throw new ServiceException(ServiceException.CONFLICT, "Package meta dir already exists: " + metaDir.getAbsolutePath());
        if (!metaDir.mkdirs())
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Cannot create meta dir: " + metaDir.getAbsolutePath());

        PackageMeta pkgMeta = new PackageMeta(packageName);
        pkgMeta.setSchemaVersion(AssetVersion.formatVersion(DataAccess.currentSchemaVersion));
        pkgMeta.setVersion(new MdwVersion(1));
        try {
            String pkgYaml = Yamlable.toString(pkgMeta, 2);
            FileHelper.writeToFile(new ByteArrayInputStream(pkgYaml.getBytes()), new File(metaDir + "/" + PackageMeta.PACKAGE_YAML));
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
        }
    }

    @Override
    public void deletePackage(String packageName) throws ServiceException {
        File dir = new File(assetRoot + "/" + packageName.replace('.', '/'));
        if (!dir.exists() || !(new File(dir + "/.mdw")).exists())
            throw new ServiceException(ServiceException.NOT_FOUND, "Package dir not found: " + dir);
        try {
            FileHelper.deleteRecursive(dir);
        }
        catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    /**
     * Create a new asset (version 1) on the file system.
     */
    @Override
    public void createAsset(String path) throws ServiceException {
        createAsset(path, (String)null);
    }

    /**
     * Create a new asset with the specified template.
     */
    @Override
    public void createAsset(String path, String template) throws ServiceException {
        if (getAsset(path) != null)
            throw new ServiceException(ServiceException.CONFLICT, "Asset already exists: " + path);

        byte[] content = new byte[0];
        if (template == null) {
            String ext = path.substring(path.lastIndexOf('.') + 1);
            template = getDefaultTemplate(ext);
        }
        if (template != null) {
            try {
                content = Templates.getBytes("assets/" + template);
                if (content == null)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Template not found: " + template);
            }
            catch (IOException ex) {
                throw new ServiceException(ServiceException.NOT_FOUND, "Error loading template: " + template, ex);
            }
        }
        createAsset(path, content);
    }

    /**
     * Create a new asset (v0.1) on the file system.
     */
    @Override
    public void createAsset(String path, byte[] content) throws ServiceException {
        int lastSlash = path.lastIndexOf('/');
        String pkg = path.substring(0, lastSlash);
        String name = path.substring(lastSlash + 1);
        File assetFile = new File(assetRoot + "/" + pkg.replace('.', '/') + "/" + name);
        try {
            FileHelper.writeToFile(new ByteArrayInputStream(content), assetFile);
            updateAssetVersion(path, "1");
        }
        catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
        }
    }

    @Override
    public String getDefaultTemplate(String assetExt) {
        if (assetExt.equals("proc"))
            return "new.proc";
        else if (assetExt.equals("task"))
            return "autoform.task";
        else
            return null;
    }

    @Override
    public void deleteAsset(String path) throws ServiceException {
        int lastSlash = path.lastIndexOf('/');
        String pkgPath = assetRoot + "/" + path.substring(0,  lastSlash).replace('.', '/');
        File assetFile = new File(pkgPath + path.substring(lastSlash));
        if (!assetFile.isFile())
            throw new ServiceException(ServiceException.NOT_FOUND, "Asset file not found: " + assetFile);
        try {
            FileHelper.deleteRecursive(assetFile);
            getVersionControl().deleteRev(assetFile);
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
        }
    }

    @Override
    public void updateAssetVersion(String assetPath, String version) throws ServiceException {
        AssetInfo asset = getAsset(assetPath);
        if (asset == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Not found: " + assetPath);
        try {
            int lastSlash = assetPath.lastIndexOf('/');
            String pkg = assetPath.substring(0, lastSlash);
            VersionProperties verProps = new VersionProperties(new File(getAssetRoot() + "/" + pkg.replace('.', '/') + "/" + PackageMeta.VERSIONS_PATH));
            verProps.setProperty(asset.getName(), "1");
            verProps.save();
        }
        catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
    }

    @Override
    public void removeAssetVersion(String assetPath) throws ServiceException {
        int slash = assetPath.lastIndexOf('/');
        if (slash == -1 || slash > assetPath.length() - 2)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Bad path: " + assetPath);
        String pkgName = assetPath.substring(0, slash);
        String assetName = assetPath.substring(slash + 1);
        File pkgDir = new File(assetRoot.getPath() + "/" + pkgName.replace('.', '/'));
        try {
            getVersionControl().deleteRev(new File(pkgDir.getPath() + "/" + assetName));
        }
        catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
    }

    /**
     * Returns either Java or Kotlin asset implementor for a class.  Null if not found.
     */
    @Override
    public AssetInfo getImplAsset(String className) throws ServiceException {
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0 && lastDot < className.length() - 1) {
            String assetRoot = className.substring(0, lastDot) + "/" + className.substring(lastDot + 1);
                AssetInfo implAsset = getAsset(assetRoot + ".java");
                if (implAsset == null)
                    implAsset = getAsset(assetRoot + ".kt");
                return implAsset;
        }
        return null;
    }

    @Override
    public Renderer getRenderer(String assetPath, String renderTo) throws ServiceException {
        AssetInfo asset = getAsset(assetPath);
        if (asset == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Asset not found: " + assetPath);
        if (renderTo.equals("html")) {
            return new HtmlRenderer(asset);
        }
        else if (renderTo.equals("txt")) {
            return new TextRenderer(asset);
        }
        else if (renderTo.equals("json")) {
            return new JsonRenderer(asset);
        }
        else if (renderTo.equals("png")) {
            return new PngRenderer(asset);
        }
        else if (renderTo.equals("pdf")) {
            return new PdfRenderer(asset);
        }
        return null;
    }

    public JSONObject getGitBranches(String[] repoUrls) throws ServiceException {
        JSONObject repositories = new JSONObject();
        repositories.put("repositories", new JSONArray());
        try {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < repoUrls.length; i++) {
                GitDiscoverer discoverer = getDiscoverer(repoUrls[i]);
                JSONArray array = repositories.getJSONArray("repositories");
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("url", repoUrls[i]);
                jsonObj.put("branches", new JSONArray(discoverer.getBranches(PropertyManager.getIntegerProperty(PropertyNames.MDW_DISCOVERY_BRANCHTAGS_MAX, 10))));
                jsonObj.put("tags", new JSONArray(discoverer.getTags(PropertyManager.getIntegerProperty(PropertyNames.MDW_DISCOVERY_BRANCHTAGS_MAX, 10))));
                array.put(jsonObj);
            }
        }
        catch (IOException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
        return repositories;
    }

    public JSONObject discoverGitAssets(String repoUrl, String branch) throws ServiceException {
        JSONObject packages = new JSONObject();
        packages.put("gitBranch", branch);
        packages.put("assetRoot", ApplicationContext.getAssetRoot());
        packages.put("packages", new JSONArray());
        try {
            GitDiscoverer discoverer = getDiscoverer(repoUrl);
            discoverer.setRef(branch);
            Map<String,PackageMeta> packageInfo = discoverer.getPackageInfo();
            for (PackageMeta pkgMeta : packageInfo.values()) {
                JSONArray array = packages.getJSONArray("packages");
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("format", "json");
                jsonObj.put("name", pkgMeta.getName());
                jsonObj.put("version", pkgMeta.getVersion());
                array.put(jsonObj);
            }
        }
        catch (IOException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
        return packages;
    }

    public GitDiscoverer getDiscoverer(String repoUrl) throws IOException {
        URL url = new URL(repoUrl);
        GitDiscoverer discoverer;
        if ("github.com".equals(url.getHost()))
            discoverer = new GitHubDiscoverer(url);
        else
            discoverer = new GitLabDiscoverer(url, true);
        return discoverer;
    }
}
