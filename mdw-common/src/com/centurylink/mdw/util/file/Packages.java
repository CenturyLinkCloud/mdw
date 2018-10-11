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
package com.centurylink.mdw.util.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

public class Packages extends TreeMap<String,File> {

    public static final String META_DIR = ".mdw";
    public static final String PACKAGE_YAML = "package.yaml";
    public static final String PACKAGE_JSON = "package.json";
    public static final String ARCHIVE = "Archive";
    public static final String MDW_BASE = "com.centurylink.mdw.base";
    public static final List<String> DEFAULT_BASE_PACKAGES = new ArrayList<>();
    static {
        DEFAULT_BASE_PACKAGES.add(MDW_BASE);
        DEFAULT_BASE_PACKAGES.add("com.centurylink.mdw.db");
        DEFAULT_BASE_PACKAGES.add("com.centurylink.mdw.node");
        DEFAULT_BASE_PACKAGES.add("com.centurylink.mdw.react");
        DEFAULT_BASE_PACKAGES.add("com.centurylink.mdw.task");
        DEFAULT_BASE_PACKAGES.add("com.centurylink.mdw.testing");
    }

    public static boolean isMdwPackage(String packageName) {
        return packageName.startsWith("com.centurylink.mdw.") &&
                !packageName.startsWith("com.centurylink.mdw.demo") &&
                !packageName.startsWith("com.centurylink.mdw.oracle") &&
                !packageName.startsWith("com.centurylink.mdw.ibmmq") &&
                !packageName.startsWith("com.centurylink.mdw.tibco") &&
                !packageName.startsWith("com.centurylink.mdw.tests.ibmmq") &&
                !packageName.startsWith("com.centurylink.mdw.tests.tibco") &&
                !packageName.startsWith("com.centurylink.mdw.internal") &&
                !packageName.startsWith("com.centurylink.mdw.ignore") &&
                !packageName.startsWith("com.centurylink.mdw.node.node_modules");
    }

    private File assetRoot;
    public File getAssetRoot() { return assetRoot; }

    public Packages(File assetRoot) throws IOException {
        this.assetRoot = assetRoot;
        List<File> packageDirs = new ArrayList<>();
        findAssetPackageDirs(getAssetRoot(), packageDirs);
        for (File packageDir : packageDirs) {
            String packageName = getAssetPath(packageDir).replace('/', '.').replace('\\', '.');
            put(packageName, packageDir);
        }
    }

    private void findAssetPackageDirs(File from, List<File> into) throws IOException {
        MdwIgnore mdwIgnore = new MdwIgnore(from);
        for (File file : from.listFiles()) {
            if (file.isDirectory() && !file.getName().equals(META_DIR) && !file.getName().equals(ARCHIVE) && !mdwIgnore.isIgnore(file)) {
                File meta = new File(file + "/" + META_DIR);
                if (meta.isDirectory() && hasPackage(meta)) {
                    if (!mdwIgnore.isIgnore(file))
                        into.add(file);
                }
                findAssetPackageDirs(file, into);
            }
        }
    }

    private boolean hasPackage(File metaDir) {
        return new File(metaDir + "/package.json").isFile() || new File(metaDir + "/package.yaml").isFile();
    }

    public List<File> getPackageDirs() {
        List<File> dirs = new ArrayList<>(values());
        dirs.sort(new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getPath().toLowerCase().compareTo(f2.getPath().toLowerCase());
            }
        });
        return dirs;
    }

    public List<String> getPackageNames() {
         List<String> packageNames = new ArrayList<>(keySet());
         Collections.sort(packageNames);
         return packageNames;
    }

    public List<File> getAssetFiles(String packageName) throws IOException {
        List<File> assetFiles = new ArrayList<>();
        File packageDir = new File(getAssetRoot() + "/" + packageName.replace('.', '/'));
        MdwIgnore mdwIgnore = new MdwIgnore(packageDir);
        for (File file : packageDir.listFiles()) {
            if (file.isFile() && !mdwIgnore.isIgnore(file))
                assetFiles.add(file);
        }
        return assetFiles;
    }

    /**
     * Whether a (relative) file path is the result of asset compilation
     * (as determined based on asset package names).
     */
    public boolean isAssetOutput(String path) {
        for (String pkg : getPackageNames()) {
            if (path.startsWith(pkg) || path.startsWith(pkg.replace('.', '/')))
                return true;
        }
        return false;
    }

    /**
     * Returns 'to' file or dir path relative to 'from' dir.
     * Result always uses forward slashes and has no trailing slash.
     */
    public String getRelativePath(File from, File to) {
        Path fromPath = Paths.get(from.getPath()).normalize().toAbsolutePath();
        Path toPath = Paths.get(to.getPath()).normalize().toAbsolutePath();
        return fromPath.relativize(toPath).toString().replace('\\', '/');
    }

    public String getAssetPath(File file) throws IOException {
        return getRelativePath(getAssetRoot(), file);
    }
}
