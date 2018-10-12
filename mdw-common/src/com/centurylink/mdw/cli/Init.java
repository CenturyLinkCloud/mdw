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
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandNames="init", commandDescription="Initialize an MDW project", separators="=")
public class Init extends Setup {

    /**
     * Existing project directory is okay (mdw-studio wizard).
     */
    private boolean allowExisting = false;

    public Init(File projectDir) {
        super(projectDir);
        project = projectDir.getName();
        allowExisting = true;
    }

    Init() {
        // cli use only
    }

    @Parameter(description="<project>", required=true)
    private String project;

    @Parameter(names="--mdw-version", description="MDW Version")
    private String mdwVersion;
    @Override
    public String getMdwVersion() throws IOException { return mdwVersion; }
    public void setMdwVersion(String version) { this.mdwVersion = version; }

    @Parameter(names="--snapshots", description="Whether to include snapshot builds")
    private boolean snapshots;
    public boolean isSnapshots() throws IOException { return snapshots; }
    public void setSnapshots(boolean snapshots) { this.snapshots = snapshots; }

    @Parameter(names="--user", description="Dev user")
    private String user = System.getProperty("user.name");
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    @Parameter(names="--eclipse", description="Generate Eclipse workspace artifacts")
    private boolean eclipse;
    public boolean isEclipse() { return eclipse; }
    public void setEclipse(boolean eclipse) { this.eclipse = eclipse; }

    @Parameter(names="--maven", description="Generate a Maven pom.xml build file")
    private boolean maven;
    public boolean isMaven() { return maven; }
    public void setMaven(boolean maven) { this.maven = maven; }

    @Parameter(names="--cloud-foundry", description="Generate a Cloud Foundry manifest.yml file")
    private boolean cloudFoundry;
    public boolean isCloudFoundry() { return cloudFoundry; }
    public void setCloudFoundry(boolean cloudFoundry) { this.cloudFoundry = cloudFoundry; }

    @Parameter(names="--spring-boot", description="Spring Boot artifact generation")
    private boolean springBoot;
    public boolean isSpringBoot() { return springBoot; }
    public void setSpringBoot(boolean springBoot) { this.springBoot = springBoot; }

    @Parameter(names="--no-update", description="Suppress base asset download")
    private boolean noUpdate;
    public boolean isNoUpdate() { return noUpdate; }
    public void setNoUpdate(boolean noUpdate) { this.noUpdate = noUpdate; }

    @Override
    public File getProjectDir() {
        return projectDir == null ? new File(project) : projectDir;
    }

    public Init run(ProgressMonitor... progressMonitors) throws IOException {
        System.out.println("Initializing " + project + "...");
        int slashIndex = project.lastIndexOf('/');
        if (slashIndex > 0)
            project = project.substring(slashIndex + 1);

        if (getProjectDir().exists() && !allowExisting) {
            if (!getProjectDir().isDirectory() || getProjectDir().list().length > 0) {
                System.err.println(getProjectDir() + " already exists and is not an empty directory");
                return this;
            }
        }
        else {
            if (!getProjectDir().isDirectory() && !getProjectDir().mkdirs())
                throw new IOException("Unable to create project dir: " + getProjectDir());
        }

        if (mdwVersion == null)
            mdwVersion = findMdwVersion(isSnapshots());
        if (configLoc == null)
            configLoc = "config";
        if (assetLoc == null)
            assetLoc = "assets";
        if (sourceGroup == null)
            sourceGroup = "com.example." + getProjectDir().getName();

        File tempZip = Files.createTempFile("mdw-templates", ".zip").toFile();
        tempZip.deleteOnExit();
        if (templateDir == null) {
            String templatesUrl = getTemplatesUrl();
            System.out.println("Retrieving templates: " + templatesUrl);
            new Download(new URL(templatesUrl), tempZip).run(progressMonitors);
        }
        else {
            System.out.println("Using templates from: " + templateDir);
            System.out.println("TEMP ZIP: " + tempZip);
            new Zip(new File(templateDir), tempZip).run(progressMonitors);
        }
        new Unzip(tempZip, getProjectDir(), false, opt -> {
            Object value = getValue(opt);
            return value == null ? false : Boolean.valueOf(value.toString());
        }).run();
        deleteDynamicTemplates();
        System.out.println("Writing: ");
        subst(getProjectDir());
        if (isSnapshots())
            updateBuildFile();
        new File(getProjectDir() + "/src/main/java").mkdirs();
        if (!isNoUpdate()) {
            Update update = new Update(getProjectDir());
            if (!new File(getAssetLoc()).isAbsolute())
                update.setAssetLoc(getProjectDir().getName() + "/" + getAssetLoc());
            update.run(progressMonitors);
        }
        else {
            // just create asset dir
            if (!getAssetRoot().isDirectory() && !getAssetRoot().mkdirs()) {
                throw new IOException("Unable to create asset root: " + getAssetRoot());
            }
        }
        if (isMaven()) {
            File buildGradle = new File(getProjectDir() + "/build.gradle");
            if (buildGradle.exists())
                buildGradle.delete();
        }
        else {
            File pomXml = new File(getProjectDir() + "/pom.xml");
            if (pomXml.exists())
                pomXml.delete();
        }
        return this;
    }

    /**
     * These will be retrieved just-in-time based on current mdw version.
     */
    private void deleteDynamicTemplates() throws IOException {
        File codegenDir = new File(getProjectDir() + "/codegen");
        if (codegenDir.exists()) {
            System.out.println("Deleting " + codegenDir);
            new Delete(codegenDir, true).run();
        }
        File assetsDir = new File(getProjectDir() + "/assets");
        if (assetsDir.exists()) {
            System.out.println("Deleting " + assetsDir);
            new Delete(assetsDir, true).run();
        }
        File configuratorDir = new File(getProjectDir() + "/configurator");
        if (configuratorDir.exists()) {
            System.out.println("Deleting " + configuratorDir);
            new Delete(configuratorDir, true).run();
        }
    }

    protected boolean needsConfig() { return false; }
}
