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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.json.JSONObject;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.dataaccess.AssetRef;

/**
 * Serves two functions: 1) as command, show asset ref with Git archive info;
 * 2) as utility, perform old-style Archiving (backup()/archive()) when importing
 * through discovery (local import).
 * Provides no recovery of deleted Archive packages as does VcsArchiver.
 */
@Parameters(commandNames="archive", commandDescription="Asset ref info (--show for contents)")
public class Archive extends Setup {

    private static final String ARCHIVE = "Archive";
    private static final String PKG_META = ".mdw/package.json";
    private static final String VERSIONS_FILE = ".mdw/versions";

    private File tempDir;
    private File assetDir;

    private List<String> packages;
    private List<File> tempPkgDirs;
    private List<Pkg> oldPkgs;

    public static void main(String[] args) throws IOException {
        JCommander cmd = new JCommander();
        List<String> archiveArgs = new ArrayList<>();;
        archiveArgs.add("archive");
        archiveArgs.add("args");
        boolean show = false;
        boolean debug = false;
        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("--show"))
                show = true;
            else if (args[i].startsWith("--debug"))
                debug = true;
            else
              archiveArgs.add(args[i]);
        }
        Archive archive = new Archive(show);
        cmd.addCommand("archive", archive);
        cmd.parse(archiveArgs.toArray(new String[0]));
        if (debug)
            archive.debug();
        if (!archive.validate())
            return;
        archive.run(Main.getMonitor());
    }

    public Archive(File assetDir, List<String> packages) {
        this.assetDir = assetDir;
        this.packages = packages;
    }

    private boolean show;
    public Archive(boolean show) {
        this.show = show;
    }

    /**
     * Command arguments = list of asset specs.
     */
    @Parameter(names="args", description="pass-thru jgit arguments", variableArity = true)
    public List<String> args = new ArrayList<>();

    /**
     * Command for displaying asset info from Git archive.
     */
    @Override
    public Archive run(ProgressMonitor... progressMonitors) throws IOException {
        File projectDir = getProjectDir();
        if (!gitExists()) {
            System.err.println("Git not found: " + projectDir + "/.git");
            return this;
        }

        Props props = new Props(projectDir, this);
        VcInfo vcInfo = new VcInfo(projectDir, props);
        DbInfo dbInfo = new DbInfo(props);
        String assetLoc = props.get(Props.ASSET_LOC);
        Checkpoint checkpoint = new Checkpoint(props.get(Props.Gradle.MAVEN_REPO_URL), vcInfo, getAssetRoot(), dbInfo);
        try {
            checkpoint.run(progressMonitors);
            System.out.println("Asset info:");
            for (String arg : args) {
                AssetRef ref = null;
                if (arg.matches(".* v[0-9\\.\\[,\\)]*$")) {
                    ref = checkpoint.retrieveRef(arg);
                    System.out.println("  db ref: " + ref);
                }
                if (ref == null) {
                    ref = checkpoint.getCurrentRef(arg);
                    System.out.println("  current ref: " + ref);
                }
                if (ref != null && show) {
                    String assetPath = getGitPath(new File(assetLoc)) + "/" + ref.getPath();
                    Git git = new Git(props.get(Props.Gradle.MAVEN_REPO_URL), vcInfo, "readFromCommit", ref.getRef(), assetPath);
                    git.run(progressMonitors); // connect
                    byte[] bytes = (byte[]) git.getResult();
                    if (isBinary(bytes))
                        System.out.println("(Binary content)");
                    else
                        System.out.println(new String(bytes));
                }
            }
        }
        catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        }

        return this;
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
     * Move replaced package(s) to Archive folder from temp (those not found in updated asset folder).
     * Intended only for local development in-flight support (other environments use Git archiving -- run()).
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
                // TODO:  Insert assets from pkg into ASSET_REF DB table
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

    private static final boolean isBinary(final byte[] bytes) {
        int expectedLength = 0;
        for (int i = 0; i < bytes.length; i++) {
            if ((bytes[i] & 0b10000000) == 0b00000000) {
                expectedLength = 1;
            } else if ((bytes[i] & 0b11100000) == 0b11000000) {
                expectedLength = 2;
            } else if ((bytes[i] & 0b11110000) == 0b11100000) {
                expectedLength = 3;
            } else if ((bytes[i] & 0b11111000) == 0b11110000) {
                expectedLength = 4;
            } else if ((bytes[i] & 0b11111100) == 0b11111000) {
                expectedLength = 5;
            } else if ((bytes[i] & 0b11111110) == 0b11111100) {
                expectedLength = 6;
            } else {
                return true;
            }
            while (--expectedLength > 0) {
                if (++i >= bytes.length) {
                    return true;
                }
                if ((bytes[i] & 0b11000000) != 0b10000000) {
                    return true;
                }
            }
        }
        return false;
    }

}
