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
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.config.YamlBuilder;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.*;
import com.centurylink.mdw.dataaccess.file.AssetFile;
import com.centurylink.mdw.dataaccess.file.GitDiffs;
import com.centurylink.mdw.dataaccess.file.GitDiffs.DiffType;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.discovery.GitDiscoverer;
import com.centurylink.mdw.discovery.GitHubDiscoverer;
import com.centurylink.mdw.discovery.GitLabDiscoverer;
import com.centurylink.mdw.model.PackageMeta;
import com.centurylink.mdw.model.asset.*;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.file.MdwIgnore;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AssetServicesImpl implements AssetServices {

    private static final StandardLogger logger = LoggerUtil.getStandardLogger();

    private File assetRoot;
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
        if (assetPath == null && getVersionControlGit() != null) {
            assetPath = getVersionControlGit().getRelativePath(assetRoot.toPath());
        }
        return assetPath;
    }

    public AssetServicesImpl() {
        assetRoot = ApplicationContext.getAssetRoot();
    }

    public AssetServicesImpl(VersionControlGit versionControlGit, File assetRoot) {
        this.versionControlGit = versionControlGit;
        this.gitRoot = versionControlGit.getLocalDir();
        this.assetRoot = assetRoot;
    }

    public VersionControl getVersionControl() throws IOException {
        return getVersionControlGit();
    }

    private VersionControlGit versionControlGit; // only set for non-standard vcs location
    private VersionControlGit getVersionControlGit() throws IOException  {
        if (versionControlGit == null) {
            try {
                return (VersionControlGit) DataAccess.getAssetVersionControl(assetRoot);
            } catch (DataAccessException ex) {
                if (ex.getCause() instanceof IOException) {
                    logger.severeException(ex.getMessage(), ex);
                    throw (IOException) ex.getCause();
                } else {
                    throw new IOException(ex.getMessage(), ex);
                }
            }
        }
        else {
            return versionControlGit;
        }
    }

    public PackageAssets getAssets(String packageName) throws ServiceException {
        return getAssets(packageName, false);
    }

    /**
     * Returns all the assets for the specified package.
     * Does not use the AssetCache.
     */
    public PackageAssets getAssets(String packageName, boolean withVcsInfo) throws ServiceException {
        try {
            PackageDir pkgDir = getPackageDir(packageName);
            if (pkgDir == null) {
                pkgDir = getGhostPackage(packageName);
                if (pkgDir == null)
                    throw new DataAccessException("Missing package metadata directory: " + packageName);
            }
            List<AssetInfo> assets = new ArrayList<>();
            if (!DiffType.MISSING.equals(pkgDir.getVcsDiffType())) {
                for (File file : Objects.requireNonNull(pkgDir.listFiles())) {
                    if (file.isFile()) {
                        AssetFile assetFile = pkgDir.getAssetFile(file);
                        if (!MdwIgnore.isIgnore(assetFile))
                            assets.add(new AssetInfo(assetFile));
                    }
                }
            }

            PackageAssets pkgAssets = new PackageAssets(pkgDir);
            pkgAssets.setAssets(assets);

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

    public Map<String,List<AssetInfo>> getAssetsOfType(String format) throws ServiceException {
        return findAssets(file -> file.getName().endsWith("." + format));
    }

    public Map<String,List<AssetInfo>> getAssetsOfTypes(String[] formats) throws ServiceException {
        Map<String,List<AssetInfo>> pkgAssets = new HashMap<>();
        for (String format: formats) {
            Map<String,List<AssetInfo>> formatAssets = getAssetsOfType(format);
            for (String pkg : formatAssets.keySet()) {
                List<AssetInfo> assets = pkgAssets.get(pkg);
                //noinspection Java8MapApi
                if (assets == null) {
                    assets = new ArrayList<>();
                    pkgAssets.put(pkg, assets);
                }
                assets.addAll(formatAssets.get(pkg));
            }
        }
        return pkgAssets;
    }

    public Map<String,List<AssetInfo>> findAssets(Predicate<File> predicate) throws ServiceException {
        Map<String,List<AssetInfo>> packageAssets = new HashMap<>();
        try {
            CodeTimer timer = new CodeTimer("AssetServices", true);
            List<PackageDir> pkgDirs = getPackageDirs();
            if (pkgDirs != null) {
                for (PackageDir pkgDir : pkgDirs) {
                    MdwIgnore mdwIgnore = new MdwIgnore(pkgDir);
                    List<AssetInfo> assets = null;
                    for (File file : pkgDir.listFiles())
                        if (file.isFile() && !mdwIgnore.isIgnore(file) && predicate.test(file)) {
                            if (assets == null)
                                assets = new ArrayList<>();
                            assets.add(new AssetInfo(pkgDir.getAssetFile(file)));
                        }
                    if (assets != null)
                        packageAssets.put(pkgDir.getPackageName(), assets);
                }
            }
            timer.logTimingAndContinue("findAssets()");
            return packageAssets;
        }
        catch (IOException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    public PackageList getPackages(boolean withVcsInfo) throws ServiceException {
        List<File> assetRoots = new ArrayList<>();
        assetRoots.add(assetRoot);
        try {
            File vcsRoot = getVersionControl() == null ? null : getGitRoot();
            PackageList pkgList = new PackageList(ApplicationContext.getServer(), assetRoot, vcsRoot);
            CodeTimer timer = new CodeTimer("AssetServices", true);
            List<PackageDir> pkgDirs = findPackageDirs(assetRoots, new ArrayList<>());
            timer.logTimingAndContinue("findPackageDirs()");
            pkgList.setPackageDirs(pkgDirs);
            if (withVcsInfo)
              addVersionControlInfo(pkgList);
            pkgList.sort();
            timer.stopAndLogTiming("addVersionControlInfo(PackageList)");
            return pkgList;
        }
        catch (DataAccessException | IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("squid:S3958")
    public AssetPackageList getAssetPackageList(Query query) throws ServiceException {
        try {
            List<PackageAssets> packageAssetList = new ArrayList<>();
            //noinspection ArraysAsListWithZeroOrOneArgument
            for (PackageDir pkgDir : findPackageDirs(Arrays.asList(assetRoot), new ArrayList<>())) {

                Stream<File> stream = Arrays.stream(pkgDir.listFiles());

                // currently "extension" is the only supported filter
                String ext = query.getFilter("extension");
                if (ext != null) {
                    if (ext.startsWith("[")) {
                        String[] exts = query.getArrayFilter("extension");
                        stream = stream.filter(f -> {
                            if (f.isFile()) {
                                for (String anExt : exts) {
                                    if (f.getName().endsWith("." + anExt))
                                        return true;
                                }
                            }
                            return false;
                        });
                    }
                    else {
                        stream = stream.filter(f -> f.isFile() && f.getName().endsWith("." + ext));
                    }
                }

                List<AssetInfo> assets = new ArrayList<>();
                for (File file : stream.collect(Collectors.toList())) {
                    assets.add(new AssetInfo(pkgDir.getAssetFile(file)));
                }
                if (!assets.isEmpty()) {
                    PackageAssets pkgAssets = new PackageAssets(pkgDir);
                    pkgAssets.setAssets(assets);
                    packageAssetList.add(pkgAssets);
                }
            }
            AssetPackageList assetPackageList = new AssetPackageList(packageAssetList);
            assetPackageList.setRetrieveDate(DatabaseAccess.getDbDate());
            assetPackageList.sort();
            return assetPackageList;
        }
        catch (DataAccessException| IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public PackageDir getPackage(String name) throws ServiceException {
        try {
            return getPackageDir(name);
        }
        catch (DataAccessException | IOException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void createPackage(String packageName) throws ServiceException {
        File dir = new File(assetRoot + "/" + packageName.replace('.', '/'));
        File metaDir = new File(dir + "/.mdw");
        if (metaDir.exists())
            throw new ServiceException(ServiceException.CONFLICT, "Package meta dir already exists: " + metaDir.getAbsolutePath());
        if (!metaDir.mkdirs())
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Cannot create meta dir: " + metaDir.getAbsolutePath());

        Package pkg = new Package();
        pkg.setSchemaVersion(DataAccess.currentSchemaVersion);
        pkg.setVersion(1);
        try {
            JSONObject json = pkg.getJson();
            json.put("name", packageName);
            FileHelper.writeToFile(new ByteArrayInputStream(new YamlBuilder(json).toString().getBytes()), new File(metaDir + "/package.yaml"));
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

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    @Override
    public List<PackageDir> getPackageDirs() throws IOException {
        try {
            return findPackageDirs(Arrays.asList(assetRoot), new ArrayList<>());
        }
        catch (DataAccessException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public List<PackageDir> findPackageDirs(Predicate<File> predicate) throws IOException {
        return getPackageDirs().stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * Finds the next level of sibling PackageDirs under a set of non-package dirs.
     */
    private List<PackageDir> findPackageDirs(List<File> dirs, List<File> excludes) throws IOException, DataAccessException {
        List<PackageDir> pkgSubDirs = new ArrayList<>();
        List<File> allSubDirs = new ArrayList<>();

        for (File dir : dirs) {
            MdwIgnore mdwIgnore = new MdwIgnore(dir);
            for (File sub : dir.listFiles()) {
                if (sub.isDirectory() && !excludes.contains(sub) && !mdwIgnore.isIgnore(sub)) {
                    if (new File(sub + "/.mdw").isDirectory()) {
                        PackageDir pkgSubDir = new PackageDir(getAssetRoot(), sub, getAssetVersionControl());
                        if (pkgSubDir.parse())
                            pkgSubDirs.add(pkgSubDir);
                    }
                    allSubDirs.add(sub);
                }
            }
        }

        if (!allSubDirs.isEmpty())
            pkgSubDirs.addAll(findPackageDirs(allSubDirs, excludes));

        return pkgSubDirs;
    }

    private void addVersionControlInfo(PackageList pkgList) {
        try {
            VersionControlGit versionControl = (VersionControlGit) getVersionControl();
            if (versionControl != null) {
                if (getGitBranch() != null)
                    pkgList.setVcsBranch(getGitBranch());
                if (getGitTag() != null)
                    pkgList.setVcsTag(getGitTag());
                if (getGitRemoteUrl() != null)
                    pkgList.setVcsRemoteUrl(getGitRemoteUrl());

                pkgList.setGitBranch(versionControl.getBranch());

                if (!pkgList.getPackageDirs().isEmpty() && getGitUser() != null && getGitBranch() != null) {
                    GitDiffs diffs = versionControl.getDiffs(getGitBranch(), getAssetPath());

                    for (PackageDir pkgDir : pkgList.getPackageDirs()) {
                        String pkgVcPath = versionControl.getRelativePath(pkgDir.toPath());
                        // check for extra packages
                        for (String extraPath : diffs.getDiffs(DiffType.EXTRA)) {
                            if (extraPath.equals(pkgVcPath + "/" + PackageDir.PACKAGE_YAML_PATH)) {
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
                        if (missingDiff.endsWith(PackageDir.PACKAGE_YAML_PATH)) {
                            int trim = PackageDir.PACKAGE_YAML_PATH.length();
                            // add a ghost package
                            String pkgName = missingDiff.substring(getAssetPath().length() + 1, missingDiff.length() - trim - 1).replace('/', '.');
                            PackageDir pkgDir = getGhostPackage(pkgName);
                            if (pkgDir != null) {
                                pkgList.getPackageDirs().add(pkgDir);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            logger.severeException("Unable to retrieve Git information for asset packages", ex);
        }
    }

    private void addVersionControlInfo(PackageAssets pkgAssets) {
        try {
            VersionControlGit versionControl = (VersionControlGit) getVersionControl();
            if (versionControl != null && getGitUser() != null && getGitBranch() != null) {
                PackageDir pkgDir = pkgAssets.getPackageDir();
                String pkgVcPath = versionControl.getRelativePath(pkgDir.toPath());
                GitDiffs diffs = versionControl.getDiffs(getGitBranch(), pkgVcPath);
                pkgDir.setVcsDiffType(diffs.getDiffType(pkgVcPath + "/" + PackageDir.META_DIR + "/" + pkgDir.getMetaFile().getName()));

                for (AssetInfo asset : pkgAssets.getAssets()) {
                    String assetVcPath = versionControl.getRelativePath(asset.getFile().toPath());
                    DiffType diffType = diffs.getDiffType(assetVcPath);
                    if (diffType != null) {
                        asset.setVcsDiffType(diffType);
                        if (pkgDir.getVcsDiffType() == null)
                          pkgDir.setVcsDiffType(DiffType.DIFFERENT);
                    }
                }

                // check for missing assets
                for (String missingDiff : diffs.getDiffs(DiffType.MISSING)) {
                    if (missingDiff.startsWith(pkgVcPath + "/") && !missingDiff.startsWith(pkgVcPath + "/.mdw")) {
                        String assetName = missingDiff.substring(pkgVcPath.length() + 1);
                        AssetInfo asset = getGhostAsset(pkgDir, assetName);
                        if (asset != null)
                          pkgAssets.getAssets().add(asset);
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

    @Override
    public AssetInfo getAsset(String path) throws ServiceException {
        int lastSlash = path.lastIndexOf('/');
        String pkgName = path.substring(0, lastSlash);
        String assetName = path.substring(lastSlash + 1);
        try {
            PackageDir pkgDir = getPackageDir(pkgName);
            if (pkgDir != null && findPackageDirs(dir -> dir.equals(pkgDir)).size() == 1) {
                MdwIgnore mdwIgnore = new MdwIgnore(pkgDir);
                File file = new File(pkgDir + "/" + assetName);
                if (!mdwIgnore.isIgnore(file)) {
                    AssetFile assetFile = pkgDir.getAssetFile(file);
                    if (assetFile.isFile()) {
                        return new AssetInfo(assetFile);
                    }
                }
            }
            return null;
        }
        catch (DataAccessException | IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public AssetInfo getAsset(String path, boolean withVcsInfo) throws ServiceException {
        try {
            int lastSlash = path.lastIndexOf('/');
            String pkgName = path.substring(0, lastSlash);
            String assetName = path.substring(lastSlash + 1);
            PackageDir pkgDir = getPackageDir(pkgName);
            if (pkgDir == null) {
                pkgDir = getGhostPackage(pkgName);
                if (pkgDir == null)
                    return null;

                // ghost package contains ghost assets
                AssetInfo asset = getGhostAsset(pkgDir, assetName);
                if (asset == null)
                    throw new DataAccessException("Missing asset file: " + path);
                return asset;
            }
            AssetFile assetFile = pkgDir.getAssetFile(new File(pkgDir + "/" + assetName));
            if (assetFile.isFile()) {
                AssetInfo asset = new AssetInfo(assetFile);
                if (withVcsInfo)
                    addVersionControlInfo(asset);
                return asset;
            }
            else {
                AssetInfo asset = getGhostAsset(pkgDir, assetName);
                if (asset == null)
                    throw new DataAccessException("Missing asset file for path: " + pkgName + "/" + assetName);
                return asset;
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * Returns either Java or Kotlin asset implementor for a class.  Null if not found.
     */
    public AssetInfo getImplAsset(String className) throws ServiceException {
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0 && lastDot < className.length() - 1) {
            String assetRoot = className.substring(0, lastDot) + "/" + className.substring(lastDot + 1);
                AssetInfo implAsset = getAsset(assetRoot + ".java");
                if (implAsset == null)
                    return getAsset(assetRoot + ".kt");
        }
        return null;
    }

    /**
     * Create a new asset (version 1) on the file system.
     */
    public void createAsset(String path) throws ServiceException {
        createAsset(path, new byte[0]);
    }

    /**
     * Create a new asset (version 1) on the file system.
     */
    public void createAsset(String path, byte[] content) throws ServiceException {
        int lastSlash = path.lastIndexOf('/');
        File assetFile = new File(assetRoot + "/" + path.substring(0,  lastSlash).replace('.', '/') + path.substring(lastSlash));
        try {
            FileHelper.writeToFile(new ByteArrayInputStream(content), assetFile);
            AssetRevision rev = new AssetRevision();
            rev.setVersion(1);
            rev.setModDate(new Date());
            getVersionControl().setRevision(assetFile, rev);
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
        }
    }

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

    private void addVersionControlInfo(AssetInfo asset) {
        CodeTimer timer = new CodeTimer("addVersionControlInfo(AssetInfo)", true);
        try {
            VersionControlGit versionControl = (VersionControlGit) getVersionControl();
            if (versionControl != null && getGitUser() != null && getGitBranch() != null) {
                String assetVcPath = versionControl.getRelativePath(asset.getFile().toPath());
                asset.setCommitInfo(versionControl.getCommitInfo(assetVcPath));
                GitDiffs diffs = versionControl.getDiffs(getGitBranch(), assetVcPath);
                asset.setVcsDiffType(diffs.getDiffType(assetVcPath));
            }
        }
        catch (Exception ex) {
            logger.severeException("Unable to retrieve Git information for asset packages", ex);
        }
        finally {
            timer.stopAndLogTiming(null);
        }
    }

    private PackageDir getGhostPackage(String pkgName) throws Exception {
        PackageDir pkgDir = null;
        String pkgPath = pkgName.replace('.', '/');
        VersionControlGit gitVc = (VersionControlGit) getVersionControl();
        if (gitVc != null && getGitUser() != null && getGitBranch() != null) {
            String pkgMetaFilePath = getAssetPath() + "/" + pkgPath + "/" + PackageDir.PACKAGE_YAML_PATH;
            GitDiffs diffs = gitVc.getDiffs(getGitBranch(), pkgMetaFilePath);
            if (DiffType.MISSING.equals(diffs.getDiffType(pkgMetaFilePath))) {
                VersionControl vc = null;
                String versionFilePath = getAssetPath() + "/" + pkgPath + "/" + PackageDir.VERSIONS_PATH;
                String versionPropsStr = gitVc.getRemoteContentString(getGitBranch(), versionFilePath);
                if (versionPropsStr != null) {
                    Properties versionProps = new Properties();
                    versionProps.load(new ByteArrayInputStream(versionPropsStr.getBytes()));
                    vc = new GhostVersionControl(versionProps);
                }
                pkgDir = new PackageDir(assetRoot, pkgName, vc);
                pkgDir.setVcsDiffType(DiffType.MISSING);
                String metaContent = ((VersionControlGit)getVersionControl()).getRemoteContentString(getGitBranch(), pkgMetaFilePath);
                if (metaContent != null) {
                    Package pkg = new Package((Map<String,Object>)new Yaml().load(metaContent));
                    pkgDir.setPackageVersion(pkg.getVersionString());
                }
            }
            else {
                throw new IOException("Cannot locate missing package meta in version control: " + pkgMetaFilePath);
            }
        }
        return pkgDir;
    }

    private AssetInfo getGhostAsset(PackageDir pkgDir, String assetName) throws Exception {
        AssetInfo asset = null;
        VersionControlGit gitVc = (VersionControlGit) getVersionControl();
        if (gitVc != null && getGitUser() != null && getGitBranch() != null) {
            String path = getAssetPath() + "/" + pkgDir.getPackageName().replace('.', '/') + "/" + assetName;
            GitDiffs diffs = gitVc.getDiffs(getGitBranch(), path);
            if (DiffType.MISSING.equals(diffs.getDiffType(path))) {
                AssetFile assetFile = pkgDir.getAssetFile(new File(path));
                asset = new AssetInfo(assetFile);
                asset.setVcsDiffType(DiffType.MISSING);
                if (pkgDir.getVcsDiffType() != DiffType.MISSING) {
                    // non-ghost pkg -- set version from remote
                    String versionFilePath = getAssetPath() + "/" + pkgDir.getPackageName() + "/" + PackageDir.VERSIONS_PATH;
                    String versionPropsStr = gitVc.getRemoteContentString(getGitBranch(), versionFilePath);
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
                logger.warn("Cannot locate missing asset in version control: " + pkgDir.getPackageName() + "/" + assetName);
            }
        }
        return asset;
    }

    public List<String> getExtraPackageNames() throws ServiceException {
        PackageList packageList = getPackages(true);

        return packageList.getPackageDirs().stream()
                .filter(packageDir -> packageDir.getVcsDiffType() == DiffType.EXTRA)
                .map(PackageDir::getPackageName).collect(Collectors.toList());
    }

    private static class GhostVersionControl implements VersionControl {

        private final Properties assetRevisions;

        GhostVersionControl(Properties assetRevisions) {
            this.assetRevisions = assetRevisions;
        }

        public AssetRevision getRevision(File file) {
            if (assetRevisions == null)
                return null;
            String val = assetRevisions.getProperty(file.getName());
            if (val == null)
                return null;
            return VersionControlGit.parseAssetRevision(val.trim());
        }

        public void connect(String repositoryUrl, String user, String password, File localDir) {
        }

        public long getId(File file) {
            return 0;
        }

        public File getFile(long id) {
            return null;
        }

        public void setRevision(File file, AssetRevision rev) {
        }

        public void clearId(File file) {
        }

        public void deleteRev(File file) {
        }

        public void clear() {
        }

        public boolean exists() {
            return false;
        }

        public void hardCheckout(String branch, Boolean hard) {
        }

        public String getCommit() {
            return null;
        }
    }

    @Override
    public Renderer getRenderer(String assetPath, String renderTo) throws ServiceException {
        AssetInfo asset = getAsset(assetPath);
        if (asset == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Asset not found: " + assetPath);
        if (renderTo.equals(Asset.HTML)) {
            return new HtmlRenderer(asset);
        }
        else if (renderTo.equals(Asset.TEXT)) {
            return new TextRenderer(asset);
        }
        else if (renderTo.equals(Asset.JSON)) {
            return new JsonRenderer(asset);
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
            Map<String, PackageMeta> pacakgeInfo = discoverer.getPackageInfo();
            for (PackageMeta pkgMeta : pacakgeInfo.values()) {
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

    public GitDiscoverer getDiscoverer(String repoUrl) throws IOException{
        URL url = new URL(repoUrl);
        GitDiscoverer discoverer;
        if ("github.com".equals(url.getHost()))
            discoverer = new GitHubDiscoverer(url);
        else
            discoverer = new GitLabDiscoverer(url, true);
        return discoverer;
    }
}
