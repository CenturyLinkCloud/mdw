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
import java.net.URL;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Imports asset packages from Git or Maven.
 * If --group-id option is specified, imports from Maven;
 * otherwise, if --packageName is specified imports from an mdw instance;
 * otherwise, imports from associated Git repository.
 * Note: this performs a Git
 * <a href="https://git-scm.com/docs/git-reset#git-reset---hard">HARD REST</a>,
 * overwriting all local changes.
 */
@Parameters(commandNames="import", commandDescription="Import assets from Git (HARD RESET!), or Maven", separators="=")
public class Import extends Setup {

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

    public Import run(ProgressMonitor... monitors) throws IOException {
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
            int index = pkg.indexOf('-');
            importPackageFromMaven(groupId, pkg.substring(0, index), pkg.substring(index + 1), monitors);
        }
    }


    public void importMdw(ProgressMonitor... monitors) throws IOException {
        List<String> pkgs = Arrays.asList(packageNames.trim().split("\\s*,\\s*"));
        importPackagesFromMdw(new Props(this).get(Props.DISCOVERY_URL), pkgs);
    }

    public void importGit(ProgressMonitor... monitors) throws IOException {
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

        DbInfo dbInfo = new DbInfo(props);
        Checkpoint checkpoint = new Checkpoint(getReleasesUrl(), vcInfo, getAssetRoot(), dbInfo);
        try {
            checkpoint.run(monitors).updateRefs();
        }
        catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        }

        Git git = new Git(getReleasesUrl(), vcInfo, "hardCheckout", vcInfo.getBranch());
        git.run(monitors);
    }

    protected void importPackageFromMaven(String groupId, String artifactId, String version,
            ProgressMonitor... monitors) throws IOException {
        File assetDir = new File(getAssetLoc());
        System.out.println("Importing from Maven into: " + assetDir + "...");
        String url = "http://search.maven.org/remotecontent?filepath=";
        List<String> pkgs = new ArrayList<>();
        pkgs.add(groupId.replace("assets", "") + artifactId.replace('-', '.'));
        File tempZip = Files.createTempFile("central-discovery", ".zip").toFile();
        new Download(new URL(url + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/"
                + artifactId + "-" + version + ".zip"), tempZip).run(monitors);

        Archive archive = new Archive(assetDir, pkgs);
        archive.backup();
        System.out.println("Unzipping into: " + assetDir);
        new Unzip(tempZip, assetDir, true).run();
        archive.archive(true);
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
        Archive archive = new Archive(assetDir, packages);
        archive.backup();
        System.out.println("Unzipping into: " + assetDir);
        new Unzip(tempZip, assetDir, true).run();
        archive.archive(true);
        if (!tempZip.delete())
            throw new IOException("Failed to delete: " + tempZip.getAbsolutePath());
    }

}
