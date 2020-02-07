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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.bpmn.BpmnProcessImporter;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.drawio.DrawIoProcessImporter;
import com.centurylink.mdw.model.Yamlable;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.procimport.ProcessImporter;

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
    private static final String MAVEN_REPO_BASE = "https://repo1.maven.org/maven2";

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

    @Parameter(names="--yaml", description="Import .proc as YAML")
    private boolean yaml;
    public boolean isYaml() { return yaml; }
    public void setYaml(boolean yaml) { this.yaml = yaml; }

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
            if (format == null) {
                // try and infer from file ext
                int lastDot = file.getName().lastIndexOf('.');
                if (lastDot > 0 && lastDot < file.getName().length() - 1) {
                    String ext = file.getName().substring(lastDot + 1);
                    if (ext.equals("bpmn"))
                        format = "bpmn";
                    else if (ext.equals("xml"))
                        format = "draw.io";
                }
            }
            if (format == null)
                throw new IOException("--format not determined from file extension; must be specified");
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
            if (!outFile.getParentFile().isDirectory() && !outFile.getParentFile().mkdirs())
                throw new IOException("Unable to create directory: " + outFile.getParentFile().getAbsolutePath());
            String procString = isYaml() ? Yamlable.toString(proc,2) : proc.getJson().toString(2);
            Files.write(outFile.toPath(), procString.getBytes());
        }
        else {
            if (!isForce()) {
                String serviceUrl = new Props(this).get(Props.SERVICES_URL, false);
                if (serviceUrl != null && new URL(serviceUrl).getHost().equals("localhost")) {
                    getErr().println(Props.SERVICES_URL.getProperty() + " indicates 'localhost'; "
                            + "use --force to confirm (overwrites ALL local changes)");
                    return this;
                }
            }
            if (groupId != null) {
                importMaven(monitors);
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
            if (!Arrays.asList(monitors).stream().anyMatch(mon -> mon.isCanceled())) {
                int index = pkg.lastIndexOf('-');
                importPackageFromMaven(groupId, pkg.substring(0, index), pkg.substring(index + 1), monitors);
            }
        }
    }

    /**
     * This is for importing project assets from Git into an environment.
     */
    public void importAssetsFromGit(ProgressMonitor... monitors) throws IOException {
        if (inProgress)
            throw new IOException("Asset import already in progress...");

        try {
            inProgress = true;

            getOut().println("Importing from Git into: " + getProjectDir() + "...(branch: " + branch + ")(Hard Reset: " + (hardReset ? "YES)" : "NO)"));

            // Check Asset inconsistencies
            Vercheck vercheck = new Vercheck();
            vercheck.setConfigLoc(getConfigLoc());
            vercheck.setAssetLoc(getAssetLoc());
            vercheck.setGitRoot(getGitRoot());
            vercheck.setForImport(true);
            vercheck.setDebug(true);
            vercheck.setOut(getOut());
            vercheck.setErr(getErr());
            vercheck.run();
            if (vercheck.getErrorCount() > 0) {
                throw new IOException("Asset version conflict(s).  See log for details");
            }

            // Perform import (Git pull)
            versionControl.hardCheckout(branch, hardReset);

            // Clear cached previous asset revisions
            versionControl.clear();

            // Capture new Refs in ASSET_REF after import (Git pull) and insert/update VALUE table
            Checkpoint checkpoint = new Checkpoint(getEngineAssetRoot(), versionControl, versionControl.getCommit(), pooledConn);
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

    /**
     * This is for importing newly-discovered assets.
     */
    public void importGit(ProgressMonitor... monitors) throws IOException {
        if (inProgress)
            throw new IOException("Import already in progress...");

        try {
            inProgress = true;
            Props props = new Props(this);
            VcInfo vcInfo = new VcInfo(getGitRoot(), props);

            getOut().println("Importing from Git into: " + getProjectDir() + "...");

            // CLI dependencies
            Git git = new Git(vcInfo, "toString");
            git.run(monitors);

            // Check Asset inconsistencies
            Vercheck vercheck = new Vercheck();
            vercheck.setConfigLoc(getConfigLoc());
            vercheck.setAssetLoc(getAssetLoc());
            vercheck.setGitRoot(getGitRoot());
            vercheck.setForImport(true);
            vercheck.setDebug(true);
            vercheck.run();
            if (vercheck.getErrorCount() > 0) {
                throw new IOException("Asset version conflict(s).  See log for details");
            }

            // perform import (Git pull)
            git = new Git(vcInfo, "hardCheckout", vcInfo.getBranch(), isHardReset());
            git.run(monitors);

            // capture new Refs in ASSET_REF after import (Git pull)
            DbInfo dbInfo = new DbInfo(props);
            Checkpoint checkpoint = new Checkpoint(vcInfo, getAssetRoot(), dbInfo);
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
        if (!assetDir.exists() && !assetDir.mkdirs())
            throw new IOException("Cannot create asset dir: " + assetDir);
        String pkg = groupId.replace("assets", "") + artifactId.replace('-', '.');
        File tempZip = Files.createTempFile("central-discovery", ".zip").toFile();
        String url = MAVEN_REPO_BASE + "/" + groupId.replace('.', '/') + "/" + artifactId + "/"
                + version + "/" + artifactId + "-" + version + ".zip";
        try {
            String msg = "Importing " + pkg + " v" + version;
            new Download(new URL(url), tempZip, msg).run(monitors);
            getOut().println("Unzipping " + artifactId + "/" + version + " into: " + assetDir);
            new Unzip(tempZip, assetDir, true).run();
            if (!tempZip.delete())
                throw new IOException("Failed to delete: " + tempZip.getAbsolutePath());
        }
        catch (FileNotFoundException e) {
            getErr().println("  - " + url + " not found for import");
        }
    }

    protected void importSnapshotPackage(String pkg, ProgressMonitor... monitors) throws IOException {
        File assetDir = new File(getAssetLoc());
        if (!assetDir.exists() && !assetDir.mkdirs())
            throw new IOException("Cannot create asset dir: " + assetDir);
        String url = SONATYPE_URL + "/redirect?r=snapshots&g=com.centurylink.mdw.assets&a="
                + pkg.replace("com.centurylink.mdw.", "").replace('.', '-') + "&v=LATEST&p=zip";
        List<String> pkgs = new ArrayList<>();
        pkgs.add(pkg);
        File tempZip = Files.createTempFile("sonatype-discovery", ".zip").toFile();
        try {
            String msg = "Importing " + pkg;
            new Download(new URL(url), tempZip, msg).run(monitors);
            getOut().println("Unzipping into: " + assetDir);
            new Unzip(tempZip, assetDir, true).run();
            if (!tempZip.delete())
                throw new IOException("Failed to delete: " + tempZip.getAbsolutePath());
        }
        catch (FileNotFoundException e) {
            getErr().println("  - " + pkg + " not found for import");
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

    private File getEngineAssetRoot() throws IOException {
        File assetRoot = getAssetRoot();
        if (assetRoot.isAbsolute())
            return assetRoot;
        else
            return new File(getProjectDir() + "/" + getAssetLoc());
    }
}
