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
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.util.file.VersionProperties;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
    public boolean isFix() { return warn; }
    public void setFix(boolean fix) { this.fix = fix; }

    private static final String ERR_UNVERSIONED = "Unversioned asset";
    private static final String ERR_BAD_VERSION_LINE = "Bad version line";
    private static final String ERR_TAG_VER_GT = "Tag version greater than asset version";
    private static final String ERR_SAME_VER_DIFF_CONTENT = "Content changed but version not incremented";
    private static final String WARN_EXTRA_VERSION = "Extraneous version entry";
    private static final String[] UNVERSIONED_EXTS = new String[] { ".impl", ".evth"  };

    private Map<String,AssetFile> assetFiles;

    private int errorCount;
    public int getErrorCount() { return errorCount; }

    @Override
    public Vercheck run(ProgressMonitor... monitors) throws IOException {

        getOut().println("Finding asset files...");

        findAssetFiles();

        try {
            compareVersions();
        }
        catch (ReflectiveOperationException | IOException ex) {
            if (isDebug())
                ex.printStackTrace();
            getErr().println("ERROR: " + ex + " (--debug for details)");
            errorCount++;
        }

        for (String path : assetFiles.keySet()) {
            AssetFile assetFile = assetFiles.get(path);
            if (assetFile.error != null) {
                if (assetFile.fixed) {
                    getOut().println("FIXED: " + path + " --> " + assetFile.error);
                }
                else {
                    getErr().println("ERROR: " + path + " --> " + assetFile.error);
                    errorCount++;
                }
            }
            else {
                if (assetFile.file == null && warn) {
                    if (fix && removeVersion(assetFile)) {
                        getErr().println("FIXED: " + path + " --> " + WARN_EXTRA_VERSION);
                    }
                    else {
                        getErr().println("WARNING: " + path + " --> " + WARN_EXTRA_VERSION);
                    }
                }
            }
        }

        if (errorCount > 0)
            getErr().println("\nversion check failed with " + errorCount + " errors");

        return this;
    }

    private void compareVersions() throws IOException, ReflectiveOperationException {
        Props props = new Props(this);
        VcInfo vcInfo = new VcInfo(getGitRoot(), props);
        String mavenUrl = props.get(Props.Gradle.MAVEN_REPO_URL);
        Git git;
        String commit;
        if (tag != null) {
            git = new Git(mavenUrl, vcInfo, "getCommitForTag", tag);
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
            git = new Git(mavenUrl, vcInfo, "getRemoteCommit", branch);
            commit = (String) git.run().getResult();
            if (commit == null)
                throw new IOException("No commit found for branch '" + branch + "'");
            getOut().println("Comparing content vs branch " + branch + " (" + commit + ")");
        }
        Map<String,Properties> gitVersions = new HashMap<>();
        VersionControl versionControl = git.getVersionControl();
        Method readFromCommit = versionControl.getClass().getMethod("readFromCommit", String.class, String.class);
        for (String path : assetFiles.keySet()) {
            AssetFile assetFile = assetFiles.get(path);
            if (assetFile.error == null && assetFile.file != null) {
                // check if version has been incremented
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
                String getVerProp = gitVerProps.getProperty(assetFile.file.getName());
                if (getVerProp != null) {
                    try {
                        int gitVer = Integer.parseInt(getVerProp.split(" ")[0]);
                        if (gitVer > assetFile.version) {
                            assetFile.error = ERR_TAG_VER_GT + " (" + formatVersion(gitVer) + " > " + formatVersion(assetFile.version) + ")";
                            if (fix)
                                assetFile.fixed = updateVersion(assetFile, ++gitVer);
                        }
                        else if (gitVer == assetFile.version) {
                            // compare file contents
                            String gitPath = getGitPath(assetFile.file);
                            byte[] tagContents = (byte[]) readFromCommit.invoke(versionControl, commit, gitPath);
                            byte[] fileContents = Files.readAllBytes(Paths.get(assetFile.file.getPath()));
                            boolean isDifferent;
                            if (assetFile.isBinary()) {
                                isDifferent = !Arrays.equals(tagContents, fileContents);
                            }
                            else {
                                String tagString =  new String(tagContents).replaceAll("\\r\\n", "\n");
                                String fileString =  new String(fileContents).replaceAll("\\r\\n", "\n");
                                isDifferent = !tagString.equals(fileString);
                            }
                            if (isDifferent) {
                                assetFile.error = ERR_SAME_VER_DIFF_CONTENT + " (v " + formatVersion(assetFile.version) + ")";
                                if (fix)
                                    assetFile.fixed = updateVersion(assetFile, ++assetFile.version);
                            }
                        }
                    }
                    catch (NumberFormatException ex) {
                        // won't compare
                    }
                }
            }
        }
    }

    private Map<String,AssetFile> findAssetFiles(ProgressMonitor... monitors) throws IOException {
        Map<String,File> packageDirs = getAssetPackageDirs();
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
                    if (assetFile.version == 0) {
                        assetFile.error = ERR_UNVERSIONED;
                        if (fix)
                            assetFile.fixed = updateVersion(assetFile, 1);
                    }
                }
                catch (NumberFormatException ex) {
                    assetFile.error = ERR_BAD_VERSION_LINE + ": " + asset + "=" + verProp;
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
                        assetFile.error = ERR_UNVERSIONED;
                        if (fix)
                            assetFile.fixed = updateVersion(assetFile, 1);
                        assetFiles.put(path, assetFile);
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

    private Map<String,Properties> getVersionProps(Map<String,File> packageDirs) throws IOException {
        Map<String,Properties> versionProps = new HashMap<>();
        for (String pkg : packageDirs.keySet()) {
            File packageDir = packageDirs.get(pkg);
            Properties props = new Properties();
            props.load(new FileInputStream(packageDir + "/" + META_DIR + "/versions"));
            versionProps.put(pkg, props);
        }
        return versionProps;
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

    private static String formatVersion(int version) {
        if (version == 0)
            return "0";
        else
            return version/1000 + "." + version%1000;
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
