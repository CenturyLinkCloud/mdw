/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.GitDiffs;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.util.file.VersionProperties;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Parameters(commandNames = "vercheck", commandDescription = "Compare asset versions to avoid errors during import", separators="=")
public class Vercheck extends Setup {

    @Parameter(names="--tag", description="Git tag to compare")
    private String tag;
    public String getTag() {
        return tag;
    }
    public void setTag(String tag) { this.tag = tag; }

    @Parameter(names="--branch", description="Git branch to compare (default = from mdw.yaml)")
    private String branch;
    public String getBranch() {
        return branch;
    }
    public void setBranch(String branch) { this.branch = branch; }

    @Parameter(names="--warn", description="Print warning output also")
    private boolean warn;
    public boolean isWarn() { return warn; }
    public void setWarn(boolean warn) { this.warn = warn; }

    @Parameter(names="--fix", description="Automatically increment versions")
    private boolean fix;
    public boolean isFix() { return fix; }
    public void setFix(boolean fix) { this.fix = fix; }

    @Parameter(names="--for-import", description="For import (opposite direction: Git version < Local = ERROR).")
    private boolean forImport;
    public boolean isForImport() { return forImport; }
    public void setForImport(boolean forImport) { this.forImport = forImport; }

    @Parameter(names="--full-scan", description="Full scan including unchanged local assets")
    private boolean fullScan;
    public boolean isFullScan() { return fullScan; }
    public void setFullScan(boolean fullScan) { this.fullScan = fullScan; }


    private static final String ERR_UNVERSIONED = "Unversioned asset";
    private static final String ERR_BAD_VERSION_LINE = "Bad version line";
    private static final String ERR_GIT_VER_GT = "Git version greater than asset version";
    private static final String ERR_GIT_VER_LT = "Git version less than asset version";
    private static final String ERR_LOCAL_DIFF_SAME_VER = "Content changed but version not incremented";
    private static final String ERR_GIT_DIFF_SAME_VER = "Git asset content differs but version not incremented";
    private static final String WARN_EXTRA_VERSION = "Extraneous version entry";
    private static final String[] UNVERSIONED_EXTS = new String[] { ".impl", ".evth"  };

    private Map<String,AssetFile> assetFiles;

    private int errorCount;
    public int getErrorCount() { return errorCount; }

    private Exception exception;
    public Exception getException() { return exception; }

    private VcInfo vcInfo;
    private String mavenUrl;
    private Props props;

    @Override
    public List<Dependency> getDependencies() throws IOException {
        return Git.getDependencies();
    }

    @Override
    public Vercheck run(ProgressMonitor... progressMonitors) throws IOException {

        getOut().println("Finding asset files...");

        for (ProgressMonitor progressMonitor : progressMonitors)
            progressMonitor.progress(0);

        findAssetFiles();

        try {
            props = new Props(this);
            vcInfo = new VcInfo(getGitRoot(), props);
            mavenUrl = props.get(Props.Gradle.MAVEN_REPO_URL);
            compareVersions(progressMonitors);
        }
        catch (ReflectiveOperationException | IOException ex) {
            this.exception = ex;
            if (isDebug())
                ex.printStackTrace(getErr());
            getErr().println("ERROR: " + ex + " (--debug for details)");
            errorCount++;
        }

        if (exception == null) {
            for (String path : assetFiles.keySet()) {
                AssetFile assetFile = assetFiles.get(path);
                if (assetFile.error != null) {
                    if (assetFile.fixed) {
                        getOut().println("FIXED: " + path + " --> " + assetFile.error);
                    } else {
                        getErr().println("ERROR: " + path + " --> " + assetFile.error);
                        errorCount++;
                    }
                } else {
                    if (assetFile.file == null && warn) {
                        if (fix && removeVersion(assetFile)) {
                            getOut().println("FIXED: " + path + " --> " + WARN_EXTRA_VERSION);
                        } else {
                            getErr().println("WARNING: " + path + " --> " + WARN_EXTRA_VERSION);
                        }
                    }
                }
            }
        }

        if (errorCount > 0)
            getErr().println("\nversion check failed with " + errorCount + " errors");
        else
            getOut().println("\nvercheck completed with no errors");

        return this;
    }

    private void compareVersions(ProgressMonitor... progressMonitors) throws IOException, ReflectiveOperationException {
        Git git;
        String commit;
        long before = System.currentTimeMillis();
        if (tag != null) {
            git = new Git(vcInfo, "getCommitForTag", tag);
            commit = (String) git.run().getResult();
            if (commit == null)
                throw new IOException("No commit found for tag '" + tag + "'");
            getOut().println("Comparing content vs tag " + tag + " (" + commit + ")");
        }
        else {
            // compare with remote HEAD
            if (branch == null)
                branch = props.get(Props.Git.BRANCH);
            if (branch == null)
                branch = "master";
            git = new Git(vcInfo, "getRemoteCommit", branch);
            commit = (String) git.run().getResult();
            if (commit == null)
                throw new IOException("No commit found for branch '" + branch + "'");
            getOut().println("Comparing content vs branch " + branch + " (" + commit + ")");
        }
        Map<String,Properties> gitVersions = new HashMap<>();
        VersionControl versionControl = git.getVersionControl();
        Method readFromCommit = versionControl.getClass().getMethod("readFromCommit", String.class, String.class);
        int i = 0;
        for (String path : assetFiles.keySet()) {
            AssetFile assetFile = assetFiles.get(path);
            GitDiffs.DiffType gitDiff = getGitDiff(assetFile);
            boolean doCheck;
            if (isForImport())
                doCheck = gitDiff == GitDiffs.DiffType.DIFFERENT;
            else
                doCheck = isFullScan() || gitDiff != null;
            if (doCheck) {
                for (ProgressMonitor progressMonitor : progressMonitors) {
                    if (progressMonitor.isSupportsMessage())
                        progressMonitor.message(path);
                }
                if ((assetFile.error == null || isFix()) && assetFile.file != null) {
                    Properties gitVerProps = gitVersions.get(assetFile.path);
                    if (gitVerProps == null) {
                        String versionFilePath = getGitPath(new File(assetFile.file.getParentFile() + "/" + META_DIR + "/versions"));
                        gitVerProps = new Properties();
                        byte[] versionBytes = (byte[]) readFromCommit.invoke(versionControl, commit, versionFilePath);
                        if (versionBytes != null) {
                            gitVerProps.load(new ByteArrayInputStream(versionBytes));
                        }
                        gitVersions.put(assetFile.path, gitVerProps);
                    }
                    String gitVerProp = gitVerProps.getProperty(assetFile.file.getName());
                    if (gitVerProp != null) {
                        try {
                            int gitVer = Integer.parseInt(gitVerProp.split(" ")[0]);
                            if (gitVer == assetFile.version) {
                                // compare file contents
                                byte[] tagContents = (byte[]) readFromCommit.invoke(versionControl, commit, getGitPath(assetFile.file));
                                byte[] fileContents = Files.readAllBytes(Paths.get(assetFile.file.getPath()));
                                boolean isDifferent;
                                if (tagContents == null) {
                                    isDifferent = false; // not in git, despite being git versions file
                                } else if (assetFile.isBinary()) {
                                    isDifferent = !Arrays.equals(tagContents, fileContents);
                                } else {
                                    String tagString = new String(tagContents).replaceAll("\\r\\n", "\n");
                                    String fileString = new String(fileContents).replaceAll("\\r\\n", "\n");
                                    isDifferent = !tagString.equals(fileString);
                                }
                                if (isDifferent) {
                                    if (isForImport()) {
                                        assetFile.error = ERR_GIT_DIFF_SAME_VER + " (v " + formatVersion(assetFile.version) + ")";
                                    } else {
                                        assetFile.error = ERR_LOCAL_DIFF_SAME_VER + " (v " + formatVersion(assetFile.version) + ")";
                                        if (fix)
                                            assetFile.fixed = updateVersion(assetFile, ++assetFile.version);
                                    }
                                }
                            } else {
                                if (isForImport()) {
                                    if (gitVer < assetFile.version) {
                                        assetFile.error = ERR_GIT_VER_LT + " (" + formatVersion(gitVer) + " < " + formatVersion(assetFile.version) + ")";
                                    }
                                } else {
                                    if (gitVer > assetFile.version) {
                                        assetFile.error = ERR_GIT_VER_GT + " (" + formatVersion(gitVer) + " > " + formatVersion(assetFile.version) + ")";
                                        if (fix)
                                            assetFile.fixed = updateVersion(assetFile, ++gitVer);
                                    }
                                }
                            }
                        } catch (NumberFormatException ex) {
                            // won't compare
                        }
                    }
                }
            }
            int prog = (int) Math.floor((double)(i * 100)/assetFiles.size());
            for (ProgressMonitor progressMonitor : progressMonitors)
                progressMonitor.progress(prog);
            i++;
        }
        for (ProgressMonitor progressMonitor : progressMonitors)
            progressMonitor.progress(100);
        if (isDebug()) {
            long secs = Math.round(((double) (System.currentTimeMillis() - before)) / 1000);
            getOut().println("Compare finished in " + secs + " s");
        }
    }

    private GitDiffs gitDiffs;
    private GitDiffs getGitDiffs() throws IOException {
        if (gitDiffs == null) {
            Git git;
            if (tag == null) {
                git = new Git(vcInfo, "getDiffs", branch, getGitPath(getAssetRoot()));
            }
            else {
                git = new Git(vcInfo, "getDiffsForTag", tag, getGitPath(getAssetRoot()));
            }
            gitDiffs = (GitDiffs) git.run().getResult();
        }
        return gitDiffs;
    }
    private GitDiffs.DiffType getGitDiff(AssetFile assetFile) throws IOException {
        if (assetFile.file == null)
            return null;
        return getGitDiffs().getDiffType(getGitPath(assetFile.file));
    }

    private Map<String,AssetFile> findAssetFiles(ProgressMonitor... monitors) throws IOException {
        Map<String,File> packageDirs = getPackageDirs();
        Map<String,Properties> versionProps = getVersionProps(packageDirs);
        assetFiles = new HashMap<>();

        for (String pkg : versionProps.keySet()) {
            Properties pkgProps = versionProps.get(pkg);
            for (String asset : pkgProps.stringPropertyNames()) {
                String path = pkg + "/" + asset;
                AssetFile assetFile = new AssetFile(path);
                assetFiles.put(path, assetFile);
                String verProp = pkgProps.getProperty(asset);
                try {
                    assetFile.version = verProp == null ? 0 : Integer.parseInt(verProp.split(" ")[0]);
                    if (!isForImport()) {
                        if (assetFile.version == 0) {
                            assetFile.error = ERR_UNVERSIONED;
                            if (fix) {
                                assetFile.fixed = updateVersion(assetFile, 1);
                                assetFile.version = 1;
                            }
                        }
                    }
                } catch (NumberFormatException ex) {
                    assetFile.error = ERR_BAD_VERSION_LINE + ": " + asset + "=" + verProp;
                    if (fix) {
                        assetFile.fixed = updateVersion(assetFile, 1);
                        assetFile.version = 1;
                    }
                }
            }
        }

        for (String pkg : packageDirs.keySet()) {
            for (File file : getAssetFiles(pkg)) {
                String path = pkg + "/" + file.getName();
                if (isVersioned(path)) {
                    AssetFile assetFile = assetFiles.get(path);
                    if (assetFile == null) {
                        assetFile = new AssetFile(path);
                        if (!isForImport()) {
                            assetFile.error = ERR_UNVERSIONED;
                            if (fix) {
                                assetFile.fixed = updateVersion(assetFile, 1);
                                assetFile.version = 1;
                            }
                            assetFiles.put(path, assetFile);
                        }
                    }
                    assetFile.file = file;
                }
            }
        }

        return assetFiles;
    }

    private boolean isVersioned(String path) {
        for (String ext : UNVERSIONED_EXTS) {
            if (path.endsWith(ext))
                return false;
        }
        return true;
    }

    private boolean updateVersion(AssetFile assetFile, int newVer) throws IOException {
        File versionsFile = new File(getAssetRoot() + "/" + assetFile.getPackagePath() + "/" + META_DIR + "/versions");
        VersionProperties versionProperties = new VersionProperties(versionsFile);
        versionProperties.setProperty(assetFile.getAssetName(), String.valueOf(newVer));
        versionProperties.save();
        return true;
    }

    private boolean removeVersion(AssetFile assetFile) throws IOException {
        File versionsFile = new File(getAssetRoot() + "/" + assetFile.getPackagePath() + "/" + META_DIR + "/versions");
        VersionProperties versionProperties = new VersionProperties(versionsFile);
        versionProperties.remove(assetFile.getAssetName());
        versionProperties.save();
        return true;
    }

    class AssetFile {
        String path;
        File file;
        Integer version;
        String error;
        boolean fixed;

        AssetFile(String path) {
            this.path = path;
        }

        String getPackagePath() {
            return path.substring(0, path.lastIndexOf('/')).replace('.', '/');
        }

        String getAssetName() {
            return path.substring(path.lastIndexOf('/') + 1);
        }

        boolean isBinary() {
            return AssetInfo.isBinary(getExtension());
        }

        String getExtension() {
            int lastDot = file.getName().lastIndexOf('.');
            if (lastDot >= 0 && file.getName().length() > lastDot + 1) {
                return file.getName().substring(0, lastDot + 1);
            }
            else {
                return null;
            }
        }
    }
}
