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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.dataaccess.VersionControl;

@Parameters(commandNames = "assets", commandDescription = "Compare asset versions to avoid errors during import", separators="=")
public class Vercheck extends Setup {

    @Parameter(names="--tag", description="Git tag to compare")
    private String tag;
    public String getTag() {
        return tag;
    }

    @Parameter(names="--warn", description="Print warning output also")
    private boolean warn;
    public boolean isWarn() { return warn; }
    public void setWarn(boolean warn) { this.warn = warn; }

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

        System.out.println("Finding asset files...");

        findAssetFiles();

        if (tag != null) {
            try {
                compareTaggedVersions();
            }
            catch (ReflectiveOperationException | IOException ex) {
                if (isDebug())
                    ex.printStackTrace();
                System.err.println("ERROR: " + ex + " (--debug for details)");
                errorCount++;
            }
        }

        for (String path : assetFiles.keySet()) {
            AssetFile assetFile = assetFiles.get(path);
            if (assetFile.error != null) {
                System.err.println("ERROR: " + path + " --> " + assetFile.error);
                errorCount++;
            }
            else {
                if (assetFile.file == null && warn)
                    System.err.println("WARNING: " + path + " --> " + WARN_EXTRA_VERSION);
            }
        }

        if (errorCount > 0)
            System.err.println("\nversion check failed with " + errorCount + " errors");

        return this;
    }

    private void compareTaggedVersions() throws IOException, ReflectiveOperationException {
        Props props = new Props(this);
        VcInfo vcInfo = new VcInfo(getGitRoot(), props);
        String mavenUrl = props.get(Props.Gradle.MAVEN_REPO_URL);
        Git git = new Git(mavenUrl, vcInfo, "getCommitForTag", tag);
        String tagCommit = (String) git.run().getResult();
        if (tagCommit == null)
            throw new IOException("No commit found for tag '" + tag + "'");
        System.out.println("Comparing content vs tag " + tag + " (" + tagCommit + ")");
        Map<String,Properties> tagVersions = new HashMap<>();
        VersionControl versionControl = git.getVersionControl();
        Method readFromCommit = versionControl.getClass().getMethod("readFromCommit", String.class, String.class);
        for (String path : assetFiles.keySet()) {
            AssetFile assetFile = assetFiles.get(path);
            if (assetFile.error == null && assetFile.file != null) {
                // check if version has been incremented
                Properties tagProps = tagVersions.get(assetFile.path);
                if (tagProps == null) {
                    String versionFilePath = getGitPath(new File(assetFile.file.getParentFile() + "/" + META_DIR + "/versions"));
                    tagProps = new Properties();
                    byte[] versionBytes = (byte[]) readFromCommit.invoke(versionControl, tagCommit, versionFilePath);
                    if (versionBytes != null) {
                        tagProps.load(new ByteArrayInputStream(versionBytes));
                    }
                    tagVersions.put(assetFile.path, tagProps);
                }
                String tagVerProp = tagProps.getProperty(assetFile.file.getName());
                if (tagVerProp != null) {
                    try {
                        int tagVer = Integer.parseInt(tagVerProp.split(" ")[0]);
                        if (tagVer > assetFile.version) {
                            assetFile.error = ERR_TAG_VER_GT + " (" + formatVersion(tagVer) + " > " + formatVersion(assetFile.version) + ")";
                        }
                        else if (tagVer == assetFile.version) {
                            // compare file contents
                            String gitPath = getGitPath(assetFile.file);
                            byte[] tagContents = (byte[]) readFromCommit.invoke(versionControl, tagCommit, gitPath);
                            byte[] fileContents = Files.readAllBytes(Paths.get(assetFile.file.getPath()));
                            if (!Arrays.equals(tagContents, fileContents)) {
                                assetFile.error = ERR_SAME_VER_DIFF_CONTENT + " (v " + formatVersion(assetFile.version) + ")";
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

    private static String formatVersion(int version) {
        if (version == 0)
            return "0";
        else
            return version/1000 + "." + version%1000;
    }

    private Map<String,AssetFile> findAssetFiles(ProgressMonitor... monitors) throws IOException {
        Map<String,File> packageDirs = getAssetPackageDirs();
        Map<String,Properties> versionProps = getVersionProps(packageDirs);
        assetFiles = new HashMap<String,AssetFile>();

        for (String pkg : versionProps.keySet()) {
            Properties pkgProps = versionProps.get(pkg);
            for (String asset : pkgProps.stringPropertyNames()) {
                String path = pkg + "/" + asset;
                AssetFile assetFile = new AssetFile(path);
                assetFiles.put(path, assetFile);
                String verProp = pkgProps.getProperty(asset);
                try {
                    assetFile.version = verProp == null ? 0 : Integer.parseInt(verProp.split(" ")[0]);
                    if (assetFile.version == 0)
                        assetFile.error = ERR_UNVERSIONED;
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

    class AssetFile {
        String path;
        File file;
        Integer version;
        String error;
        AssetFile(String path) {
            this.path = path;
        }
    }
}
