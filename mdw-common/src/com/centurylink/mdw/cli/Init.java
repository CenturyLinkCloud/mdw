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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandNames="init", commandDescription="Initialize an MDW project", separators="=")
public class Init extends Setup {

    public Init(File projectDir) {
        super(projectDir);
        project = projectDir.getName();
    }

    Init() {
        // cli use only
    }

    @Parameter(description="<project>", required=true)
    private String project;

    @Parameter(names="--user", description="Dev user")
    private String user = System.getProperty("user.name");
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    @Parameter(names="--eclipse", description="Generate Eclipse workspace artifacts")
    private boolean eclipse = true;
    public boolean isEclipse() { return eclipse; }
    public void setEclipse(boolean eclipse) { this.eclipse = eclipse; }

    @Parameter(names="--maven", description="Generate a Maven pom.xml build file")
    private boolean maven = false;
    public boolean isMaven() { return maven; }
    public void setMaven(boolean maven) { this.maven = maven; }

    @Parameter(names="--cloud-foundry", description="Generate a Cloud Foundry manifest.yml file")
    private boolean cloudFoundry = false;
    public boolean isCloudFoundry() { return cloudFoundry; }
    public void setCloudFoundry(boolean cloudFoundry) { this.cloudFoundry = cloudFoundry; }

    @Parameter(names="--spring-boot", description="Generate Spring Boot build artifacts (currently only Gradle)")
    private boolean springBoot = false;
    public boolean isSpringBoot() { return springBoot; }
    public void setSpringBoot(boolean springBoot) { this.springBoot = springBoot; }

    @Override
    public File getProjectDir() {
        return projectDir == null ? new File(project) : projectDir;
    }

    public Init run(ProgressMonitor... progressMonitors) throws IOException {
        System.out.println("Initializing " + project + "...");
        int slashIndex = project.lastIndexOf('/');
        if (slashIndex > 0)
            project = project.substring(slashIndex + 1);

        if (getProjectDir().exists()) {
            if (!getProjectDir().isDirectory() || getProjectDir().list().length > 0) {
                System.err.println(getProjectDir() + " already exists and is not an empty directory");
                return this;
            }
        }
        else {
            if (!getProjectDir().mkdirs())
                throw new IOException("Unable to create destination: " + getProjectDir());
        }

        findMdwVersion();

        String templates = "mdw-templates-" + getMdwVersion() + ".zip";
        String templatesUrl;
        if (isSnapshots())
            templatesUrl = "https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=com.centurylink.mdw&a=mdw-templates&v=LATEST&p=zip";
        else
            templatesUrl = getReleasesUrl() + "/com/centurylink/mdw/mdw-templates/" + getMdwVersion() + "/" + templates;
        System.out.println("Retrieving templates: " + templates);
        File tempZip = Files.createTempFile("mdw-templates", ".zip").toFile();
        new Download(new URL(templatesUrl), tempZip).run(progressMonitors);
        new Unzip(tempZip, getProjectDir(), false, opt -> {
            Object value = getValue(opt);
            return value == null ? false : Boolean.valueOf(value.toString());
        }).run();
        System.out.println("Writing: ");
        subst(getProjectDir());
        new File(getProjectDir() + "/src/main/java").mkdirs();

        new Update(getProjectDir()).run(progressMonitors);
        return this;
    }
}
