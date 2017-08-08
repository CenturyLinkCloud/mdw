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
package com.centurylink.mdw.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.json.JSONObject;

/**
 * This is not for VCS asset imports; only for discovery.
 * Provides no recovery of deleted Archive packages as does VcsArchiver.
 */
public class Archive {

    private static final String ARCHIVE = "Archive";
    private static final String PKG_META = ".mdw/package.json";
    private static final String VERSIONS_FILE = ".mdw/versions";

    private File tempDir;
    private File assetDir;

    private List<String> packages;
    private List<File> tempPkgDirs;
    private List<Pkg> oldPkgs;

    public Archive(File assetDir) {
        this.assetDir = assetDir;
        packages = new ArrayList<>();
        findPackages(assetDir);
    }

    public Archive(File assetDir, List<String> packages) {
        this.assetDir = assetDir;
        this.packages = packages;
    }

    /**
     * Save asset packages to temp dir.
     */
    public void backup() throws IOException {

        List<Pkg> pkgs = oldPkgs = getPkgs(packages);
        tempDir = Files.createTempDirectory("mdw-archive").toFile();

        // copy packages due for import from the asset dir to the temp dir
        System.out.println("Back up existing...");
        tempPkgDirs = new ArrayList<>();
        for (Pkg pkg : pkgs) {
            System.out.println("  - " + pkg);
            File tempPkgDir = new File(tempDir + "/" + pkg);
            new Copy(pkg.dir, tempPkgDir, true).run();
            tempPkgDirs.add(tempPkgDir);
        }
    }

    public void archive() throws IOException {
        archive(false);
    }

    /**
     * Move replaced package(s) to archive from temp (those not found in updated asset folder).
     */
    public void archive(boolean deleteBackups) throws IOException {
        if (tempPkgDirs == null)
            throw new IOException("Backup() must be run before archive()");
        List<Pkg> newPkgs = getPkgs(packages);
        System.out.println("Checking for asset version consistency...");
        String flaggedAsset = null;
        for (Pkg prevPkg : oldPkgs) {
            for (Pkg newPkg : newPkgs) {
                if (prevPkg.name.equals(newPkg.name)) {
                    for (String key : prevPkg.assetsVer.stringPropertyNames()) {
                        if (newPkg.assetsVer.getProperty(key) != null ) {
                            String propVal = prevPkg.assetsVer.getProperty(key);
                            int firstSpace = propVal.indexOf(' ');
                            int oldVer = firstSpace > 0 ? Integer.parseInt(propVal.substring(0, firstSpace)) : Integer.parseInt(propVal);
                            propVal = newPkg.assetsVer.getProperty(key);
                            firstSpace = propVal.indexOf(' ');
                            int newVer = firstSpace > 0 ? Integer.parseInt(propVal.substring(0, firstSpace)) : Integer.parseInt(propVal);
                            if (oldVer > newVer)
                                flaggedAsset = newPkg.dir + "/" + key;
                        }
                        if (flaggedAsset != null)
                            break;
                    }
                }
                if (flaggedAsset != null)
                    break;
            }
            if (flaggedAsset != null)
                break;
        }
        if (flaggedAsset != null) {
            System.out.println("Restoring backed up assets...");
            for (Pkg pkg : oldPkgs) {
                System.out.println("  - " + pkg);
                File restorePkgFrom = new File(tempDir + "/" + pkg);
                if (pkg.dir.exists())
                    new Delete(pkg.dir, true).run();
                new Copy(restorePkgFrom, pkg.dir, true).run();
            }
            if (deleteBackups) {
                removeBackups();
            }
            throw new IOException("Cannot perform asset import, asset " + flaggedAsset + " cannot be an older version than existing");
        }

        System.out.println("Archiving....");
        for (File tempPkgDir : tempPkgDirs) {
            boolean found = false;
            for (Pkg newPkg : newPkgs) {
                if (tempPkgDir.getName().equals(newPkg.toString())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("  - " + tempPkgDir.getName());
                File archiveDest = new File(getArchiveDir() + "/" + tempPkgDir.getName());
                if (archiveDest.exists())
                    new Delete(archiveDest).run();
                new Copy(tempPkgDir, archiveDest).run();
            }
        }

        if (deleteBackups) {
            removeBackups();
        }
    }

    private void removeBackups() throws IOException {
        System.out.println("Removing temp backups...");
        new Delete(tempDir, true).run();
    }

    private void findPackages(File dir) {
        if (new File(dir + "/" + PKG_META).isFile()) {
            packages.add(dir.getAbsolutePath().substring(assetDir.getAbsolutePath().length() - 1).replace('/', '.').replace('\\', '.'));
        }
        for (File child : dir.listFiles()) {
            if (child.isDirectory())
                findPackages(child);
        }
    }

    private List<Pkg> getPkgs(List<String> packages) throws IOException {
        List<Pkg> pkgs = new ArrayList<>();
        for (String pkgName : packages) {
            File dir = new File(assetDir + "/" + pkgName.replace('.', '/'));
            if (dir.isDirectory()) {
                File pkgMeta = new File(dir + "/" + PKG_META);
                JSONObject pkgJson = new JSONObject(new String(Files.readAllBytes(Paths.get(pkgMeta.getPath()))));
                String ver = pkgJson.getString("version");
                Properties assetsVer = new Properties();
                File propFile = new File(dir + "/" + VERSIONS_FILE);
                if (propFile.exists()) {
                    InputStream in = null;
                    try {
                        in = new FileInputStream(propFile);
                        assetsVer.load(in);
                    }
                    finally {
                        if (in != null)
                            in.close();
                    }
                }
                pkgs.add(new Pkg(pkgName, dir, ver, assetsVer));
            }
        }
        Collections.sort(pkgs, new Comparator<Pkg>() {
            public int compare(Pkg pkg1, Pkg pkg2) {
                if (pkg1.name.split(".").length < pkg2.name.split(".").length)
                    return -1;
                else if (pkg1.name.split(".").length > pkg2.name.split(".").length)
                    return 1;
                else
                    return pkg1.name.compareTo(pkg2.name);
            }
        });
        return pkgs;
    }

    /**
     * Creates if necessary.
     */
    private File getArchiveDir() throws IOException {
        File archiveDir = new File(assetDir + "/" + ARCHIVE);
        if (!archiveDir.exists())
            Files.createDirectories(Paths.get(archiveDir.getPath()));
        return archiveDir;
    }

    private class Pkg {
        private String name;
        private File dir;
        private String ver;
        Properties assetsVer;

        private Pkg(String name, File dir, String ver, Properties assetsVer) {
            this.name = name;
            this.dir = dir;
            this.ver = ver;
            this.assetsVer = assetsVer;
        }

        public String toString() {
            return name + " v" + ver;
        }
    }
}
