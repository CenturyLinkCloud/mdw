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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.bpmn.BpmnProcessImporter;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.drawio.DrawIoProcessImporter;
import com.centurylink.mdw.procimport.ProcessImporter;
import com.centurylink.mdw.model.workflow.Process;

/**
 * Imports asset packages from Git or Maven.
 * If --group-id option is specified, imports from Maven;
 * otherwise, if --packageName is specified imports from an mdw instance;
 * otherwise, imports from associated Git repository.
 * Note: this performs a Git
 * <a href="https://git-scm.com/docs/git-reset#git-reset---hard">HARD RESET only if --hard-reset is specified</a>,
 * overwriting all local changes.
 */
@Parameters(commandNames="import", commandDescription="Import packages from Git/Maven, or process from external format", separators="=")
public class Import extends Setup {

    private static boolean inProgress = false;

    @Parameter(names="--file", description="File to import into process")
    private File file;
    public File getFile() { return file; }
    public void setFile(File file) { this.file = file; }

    @Parameter(names="--format", description="Process import source format")
    private String format;
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    @Parameter(names="--process", description="Destination process asset path")
    private String process;
    public String getProcess() { return process; }
    public void setProcess(String process) { this.process = process; }

    @Parameter(names="--group-id", description="Maven group id.  If this option is specified, imports from Discovery.")
    private String groupId;
    public String getGroupId() {
        return groupId;
    }

    @Parameter(names="--artifact-id", description="Maven artifact id")
    private String artifactId;
    public String getArtifactId() {
        return artifactId;
    }

    @Parameter(names="--version", description="Maven artifact version")
    private String version;
    public String getVersion() {
        return version;
    }

    @Parameter(names="--package-names", description="Packages to import from an MDW instance (comma-delimited).")
    private String packageNames;
    public String getPackageNames() { return packageNames; }

    @Parameter(names="--force", description="Force overwrite, even on localhost or when branch disagrees")
    private boolean force = false;
    public boolean isForce() { return force; }
    public void setForce(boolean force) { this.force = force; }

    @Parameter(names="--hard-reset", description="Git hard reset overwrites any and all changes in local repo")
    private boolean hardReset = false;
    public boolean isHardReset() { return hardReset; }
    public void setHardReset(boolean hardReset) { this.hardReset = hardReset; }

    // for maven
    private List<String> artifacts = new ArrayList<>();
    public List<String> getArtifacts() {
        return artifacts;
    }
    public void setArtifacts(List<String> artifacts) {
        this.artifacts = artifacts;
    }

    Import() {
        // cli only
    }

    public Import(String groupId, List<String> artifacts) {
        this.groupId = groupId;
        this.artifacts = artifacts;
    }

    public Import(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    private VersionControl versionControl = null;
    private String branch = null;
    private Connection pooledConn = null;
    public Import(File projectDir, VersionControl vc, String branch, boolean hardReset, Connection conn) {
        super(projectDir);
        versionControl = vc;
        this.branch = branch;
        this.hardReset = hardReset;
        pooledConn = conn;
    }

    public Import run(ProgressMonitor... monitors) throws IOException {
        if (file != null) {
            // process import
            if (format == null)
                throw new IOException("--format is required");
            ProcessImporter importer = getProcessImporter(format);
            File outFile;
            if (process == null) {
                int lastDot = file.getName().lastIndexOf('.');
                outFile = new File(file.getName().substring(0, lastDot) + ".proc");
            }
            else {
                outFile = getAssetFile(process);
            }
            Process proc = importer.importProcess(file);
            Files.write(outFile.toPath(), proc.getJson().toString(2).getBytes());
        }
        else {
            if (!isForce()) {
                String serviceUrl = new Props(this).get(Props.SERVICES_URL, false);
                if (serviceUrl != null && new URL(serviceUrl).getHost().equals("localhost")) {
                    System.err.println(Props.SERVICES_URL.getProperty() + " indicates 'localhost'; "
                            + "use --force to confirm (overwrites ALL local changes)");
                    return this;
                }
            }
            if (groupId != null) {
                importMaven(monitors);
            }
            else if (packageNames != null) {
                importMdw(monitors);
            }
            else {
                importGit(monitors);
            }
        }
        return this;
    }

    public void importMaven(ProgressMonitor... monitors) throws IOException {
        if (artifactId != null && version != null)
            importPackageFromMaven(groupId, artifactId, version, monitors);
        else
            importPackagesFromMaven(groupId, artifacts, monitors);
    }
    public void importPackagesFromMaven(String groupId, List<String> packages, ProgressMonitor... monitors) throws IOException {
        for (String pkg : packages) {
            int index = pkg.lastIndexOf('-');
            importPackageFromMaven(groupId, pkg.substring(0, index), pkg.substring(index + 1), monitors);
        }
    }


    public void importMdw(ProgressMonitor... monitors) throws IOException {
        List<String> pkgs = Arrays.asList(packageNames.trim().split("\\s*,\\s*"));
        importPackagesFromMdw(new Props(this).get(Props.DISCOVERY_URL), pkgs);
    }

    public void importMDWGit(ProgressMonitor... monitors) throws IOException {
        if (inProgress)
            throw new IOException("Asset import was NOT performed since an import was already in progress...");

        try {
            inProgress = true;

            System.out.println("Importing from Git into: " + getProjectDir() + "...(branch: " + branch + ")(Hard Reset: " + (hardReset ? "YES)" : "NO)"));

            // Check Asset inconsistencies
            Map<String, List<String>> issues = versionControl.checkVersionConsistency(branch, getAssetLoc());

            // Report inconsistencies if found
            if (issues != null && !issues.isEmpty()) {
                String message = "";
                if (issues.get("Old") != null) {
                    message += "Failed -- Import would replace the following assets with older versions: \n";
                    for (String file : issues.get("Old"))
                        message += file + " \n";
                }
                if (issues.get("Conflict") != null) {
                    message += "Failed -- Import would replace the following modified assets with same versions (version needs incrementing): \n";
                    for (String file : issues.get("Conflict"))
                        message += file + " \n";
                }
                throw new IOException(message);
            }

            // Perform import (Git pull)
            versionControl.hardCheckout(branch, hardReset);

            // Clear cached previous asset revisions
            versionControl.clear();

            // Capture new Refs in ASSET_REF after import (Git pull) and insert/update VALUE table
            Checkpoint checkpoint = new Checkpoint(new File(getProjectDir() + "/" + getAssetLoc()), versionControl, versionControl.getCommit(), pooledConn);
            try {
                checkpoint.updateRefs(true);
            }
            catch (SQLException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
        }
        catch (Throwable ex) {
            if (ex instanceof IOException)
                throw (IOException)ex;
            else
                throw new IOException(ex.getMessage(), ex);
        }
        finally {
            inProgress = false;
        }
    }

    public void importGit(ProgressMonitor... monitors) throws IOException {
        if (inProgress)
            throw new IOException("Asset import was NOT performed since an import was already in progress...");

        try {
            inProgress = true;
            Props props = new Props(this);
            VcInfo vcInfo = new VcInfo(getGitRoot(), props);

            if (!isForce()) {
                String configuredBranch = props.get(Props.Git.BRANCH);
                Git git = new Git(props.get(Props.Gradle.MAVEN_REPO_URL), vcInfo, "getBranch");
                git.run(monitors);
                String gitBranch = (String)git.getResult();
                if (!gitBranch.equals(configuredBranch)) {
                    System.err.println(Props.Git.BRANCH.getProperty() + " (" + configuredBranch
                            + ") disagrees with local Git (" + gitBranch
                            + ");  use --force to confirm (overwrites ALL local changes from Git remote)");
                    return;
                }
            }

            System.out.println("Importing from Git into: " + getProjectDir() + "...");

            // Capture current Refs in ASSET_REF before import
    //        DbInfo dbInfo = new DbInfo(props);
    //        Checkpoint checkpoint = new Checkpoint(getReleasesUrl(), vcInfo, getAssetRoot(), dbInfo);
    //        try {
    //            checkpoint.run(monitors).updateRefs();
    //        }
    //        catch (SQLException ex) {
    //            throw new IOException(ex.getMessage(), ex);
    //        }

            // Check Asset inconsistencies
            Git git = new Git(getReleasesUrl(), vcInfo, "checkVersionConsistency", vcInfo.getBranch(), getAssetLoc());
            git.run(monitors);
            @SuppressWarnings("unchecked")
            Map<String, List<String>> issues = (Map<String, List<String>>)git.getResult();

            // Report inconsistencies if found
            if (issues != null && !issues.isEmpty()) {
                String message = "";
                if (issues.get("Old") != null) {
                    message += "Failed -- Import would replace the following assets with older versions: \n";
                    for (String file : issues.get("Old"))
                        message += file + " \n";
                }
                if (issues.get("Conflict") != null) {
                    message += "Failed -- Import would replace the following modified assets with same versions (version needs incrementing): \n";
                    for (String file : issues.get("Conflict"))
                        message += file + " \n";
                }
                throw new IOException(message);
            }

            // Perform import (Git pull)
            git = new Git(getReleasesUrl(), vcInfo, "hardCheckout", vcInfo.getBranch(), isHardReset());
            git.run(monitors);

            // Capture new Refs in ASSET_REF after import (Git pull)
            DbInfo dbInfo = new DbInfo(props);
            Checkpoint checkpoint = new Checkpoint(getReleasesUrl(), vcInfo, getAssetRoot(), dbInfo);
            try {
                checkpoint.run(monitors).updateRefs();
            }
            catch (SQLException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
        }
        catch (Throwable ex) {
            if (ex instanceof IOException)
                throw ex;
            else
                throw new IOException(ex.getMessage(), ex);
        }
        finally {
            inProgress = false;
        }
    }

    protected void importPackageFromMaven(String groupId, String artifactId, String version,
            ProgressMonitor... monitors) throws IOException {
        File assetDir = new File(getAssetLoc());
        String url = "http://search.maven.org/remotecontent?filepath=";
        List<String> pkgs = new ArrayList<>();
        pkgs.add(groupId.replace("assets", "") + artifactId.replace('-', '.'));
        File tempZip = Files.createTempFile("central-discovery", ".zip").toFile();
        System.out.println("Importing " + artifactId + "/" + version + " from Maven into: " + assetDir + "...");
        new Download(new URL(url + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/"
                + artifactId + "-" + version + ".zip"), tempZip).run(monitors);

        System.out.println("Unzipping " + artifactId + "/" + version + " into: " + assetDir);
        new Unzip(tempZip, assetDir, true).run();
        if (!tempZip.delete())
            throw new IOException("Failed to delete: " + tempZip.getAbsolutePath());
    }

    protected void importPackagesFromMdw(String url, List<String> packages, ProgressMonitor... monitors) throws IOException {
         // download packages temp zip
        System.out.println("Downloading packages...");

        File tempZip = Files.createTempFile("mdw-discovery", ".zip").toFile();

        String pkgsParam = "[";
        for (int i = 0; i < packages.size(); i++) {
            pkgsParam += packages.get(i);
            if (i < packages.size() - 1)
                pkgsParam += ",";
        }
        pkgsParam += "]";
        new Download(new URL(url + "/asset/packages?recursive=false&packages=" + pkgsParam), tempZip).run(monitors);

        // import packages
        File assetDir = getAssetRoot();
        System.out.println("Unzipping into: " + assetDir);
        new Unzip(tempZip, assetDir, true).run();
        if (!tempZip.delete())
            throw new IOException("Failed to delete: " + tempZip.getAbsolutePath());
    }

    protected void importSnapshotPackage(String pkg, ProgressMonitor... monitors) {
        File assetDir = new File(getAssetLoc());
        System.out.println("Importing " + pkg + " from Maven into: " + assetDir + "...");
        String url = SONATYPE_URL + "/redirect?r=snapshots&g=com.centurylink.mdw.assets&a="
                + pkg.replace("com.centurylink.mdw.", "").replace('.', '-') + "&v=LATEST&p=zip";
        List<String> pkgs = new ArrayList<>();
        pkgs.add(pkg);
        try {
            File tempZip = Files.createTempFile("sonatype-discovery", ".zip").toFile();
            new Download(new URL(url), tempZip).run(monitors);

            System.out.println("Unzipping into: " + assetDir);
            new Unzip(tempZip, assetDir, true).run();
            if (!tempZip.delete())
                throw new IOException("Failed to delete: " + tempZip.getAbsolutePath());
        }
        catch (FileNotFoundException e) {
            System.err.println("  - " + pkg + " not found for import");
        }
        catch (IOException e) {
            System.err.println("Unable to import  - " + pkg );
        }
    }

    protected ProcessImporter getProcessImporter(String format) throws IOException {
        if ("bpmn".equals(format))
            return new BpmnProcessImporter();
        else if ("draw.io".equals(format))
            return new DrawIoProcessImporter();
        else
            throw new IOException("Unsupported format: " + format);
    }
}
