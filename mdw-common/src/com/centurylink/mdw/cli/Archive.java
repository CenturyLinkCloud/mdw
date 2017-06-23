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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

/**
 * This is not for VCS asset imports; only for discovery.
 * Provides no recovery of deleted Archive packages as does VcsArchiver.
 */
public class Archive {

    private static final String ARCHIVE = "Archive";
    private static final String PKG_META = ".mdw/package.json";

    private File tempDir;
    private File assetDir;

    private List<String> packages;
    private List<File> tempPkgDirs;

    public Archive(File assetDir) {
        this.assetDir = assetDir;
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

        List<Pkg> pkgs = getPkgs(packages);
        tempDir = Files.createTempDirectory("mdw-archive").toFile();

        // copy packages due for import from the asset dir to the temp dir
        System.out.println("back up existing...");
        for (Pkg pkg : pkgs) {

            System.out.println("  - " + pkg);
            File tempPkgDir = new File(tempDir + "/" + pkg);
            Files.copy(Paths.get(pkg.dir.getPath()), Paths.get(tempPkgDir.getPath()));
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
        List<Pkg> newPkgs = getPkgs(packages);
        for (File tempPkgDir : tempPkgDirs) {
            boolean found = false;
            for (Pkg newPkg : newPkgs) {
                if (tempPkgDir.getName().equals(newPkg.toString())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("  - archiving: " + tempPkgDir.getName());
                File archiveDest = new File(getArchiveDir() + "/" + tempPkgDir.getName());
                if (archiveDest.exists())
                    new Delete(archiveDest).run();
                Files.copy(Paths.get(tempPkgDir.getPath()), Paths.get(archiveDest.getPath()));
            }
        }

        if (deleteBackups) {
            System.out.println("Removing temp backups...");
            new Delete(tempDir).run();
        }
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
            File pkgMeta = new File(dir + "/" + PKG_META);
            JSONObject pkgJson = new JSONObject(new String(Files.readAllBytes(Paths.get(pkgMeta.getPath()))));
            String ver = pkgJson.getString("version");
            pkgs.add(new Pkg(pkgName, dir, ver));
        }
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

        private Pkg(String name, File dir, String ver) {
            this.name = name;
            this.dir = dir;
            this.ver = ver;
        }

        public String toString() {
            return name + " v" + ver;
        }
    }
}
