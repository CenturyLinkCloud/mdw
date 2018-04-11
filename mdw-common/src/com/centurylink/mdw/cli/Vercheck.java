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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandNames = "assets", commandDescription = "Asset version checks", separators="=")
public class Vercheck extends Setup {

    @Parameter(names="--tag", description="Git tag to compare")
    private String tag;
    public String getTag() {
        return tag;
    }

    private static final String ERR_UNVERSIONED = "Unversioned asset";
    private static final String ERR_BAD_VERSION_LINE = "Bad version line";
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
            Props props = new Props(this);
            VcInfo vcInfo = new VcInfo(getGitRoot(), props);
            Git git = new Git(props.get(Props.Gradle.MAVEN_REPO_URL), vcInfo, "getCommitForTag", tag);
            git.run(monitors); // connect
            String tagCommit = (String) git.getResult();
            System.out.println("TAG COMMIT: " + tagCommit);
        }

        for (String path : assetFiles.keySet()) {
            AssetFile assetFile = assetFiles.get(path);
            if (assetFile.error != null) {
                System.err.println("ERROR: " + path + " --> " + assetFile.error);
                errorCount++;
            }
            else {
                if (assetFile.file == null)
                    System.err.println("warning: " + path + " --> " + WARN_EXTRA_VERSION);
            }
        }

        if (errorCount > 0)
            System.out.println("\nversion check failed with " + errorCount + " errors");

        return this;
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
