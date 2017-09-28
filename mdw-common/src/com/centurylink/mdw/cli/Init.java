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

    public Init(String project) {
        this.project = project;
    }

    Init() {
        // cli use only
    }

    @Parameter(description="<project>", required=true)
    private String project;

    @Parameter(names="--snapshots", description="Whether to include snapshot builds")
    private boolean snapshots;
    public boolean isSnapshots() { return snapshots; }
    public void setSnapshots(boolean snapshots) { this.snapshots = snapshots; }

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

    public Init run(ProgressMonitor... progressMonitors) throws IOException {
        System.out.println("Initializing " + project + "...");
        projectDir = new File(project);
        int slashIndex = project.lastIndexOf('/');
        if (slashIndex > 0)
            project = project.substring(slashIndex + 1);

        if (projectDir.exists()) {
            if (!projectDir.isDirectory() || projectDir.list().length > 0)
                throw new IOException(projectDir + " already exists and is not an empty directory");
        }
        else {
            if (!projectDir.mkdirs())
                throw new IOException("Unable to create destination: " + projectDir);
        }

        String releasesUrl = getReleasesUrl();
        if (!releasesUrl.endsWith("/"))
            releasesUrl += "/";

        if (getMdwVersion() == null) {
            // find latest non-snapshot
            URL url = new URL(releasesUrl + "com/centurylink/mdw/mdw-templates/");
            Crawl crawl = new Crawl(url, snapshots);
            crawl.run();
            if (crawl.getReleases().size() == 0)
                throw new IOException("Unable to locate MDW releases: " + url);
            setMdwVersion(crawl.getReleases().get(crawl.getReleases().size() - 1));
        }

        String templates = "mdw-templates-" + getMdwVersion() + ".zip";
        String templatesUrl;
        if (isSnapshots())
            templatesUrl = "https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=com.centurylink.mdw&a=mdw-templates&v=LATEST&p=zip";
        else
            templatesUrl = releasesUrl + "com/centurylink/mdw/mdw-templates/" + getMdwVersion() + "/" + templates;
        System.out.println("Retrieving templates: " + templates);
        File tempZip = Files.createTempFile("mdw-templates", ".zip").toFile();
        new Download(new URL(templatesUrl), tempZip).run(progressMonitors);
        new Unzip(tempZip, projectDir, false, opt -> {
            Object value = getValue(opt);
            return value == null ? false : Boolean.valueOf(value.toString());
        }).run();
        System.out.println("Writing: ");
        subst(projectDir);

        return this;
    }
}
