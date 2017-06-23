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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandNames="init", commandDescription="Initialize an MDW project", separators="=")
public class Init extends Common {

    public Init(String project) {
        this.project = project;
    }

    Init() {
        // cli use only
    }

    @Parameter(description="<project>", required=true)
    private String project;

    @Parameter(names="--releases-url", description="MDW Releases Maven Repo URL")
    private String releasesUrl = "http://repo.maven.apache.org/maven2";
    public String getReleasesUrl() { return releasesUrl; }
    public void setReleasesUrl(String url) { this.releasesUrl = url; }

    @Parameter(names="--snapshots", description="Whether to include snapshot builds")
    private boolean snapshots;
    public boolean isSnapshots() { return snapshots; }
    public void setSnapshots(boolean snapshots) { this.snapshots = snapshots; }

    @Parameter(names="--user", description="Dev user")
    private String user = System.getProperty("user.name");
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    @Parameter(names="--for-eclipse", description="Generate Eclipse workspace artifacts")
    private boolean forEclipse = true;
    public boolean isForEclipse() { return forEclipse; }
    public void setForEclipses(boolean forEclipse) { this.forEclipse = forEclipse; }

    public void run() throws IOException {
        System.out.println("initializing " + project + "...");
        projectDir = new File(project);
        if (projectDir.exists()) {
            if (!projectDir.isDirectory() || projectDir.list().length > 0)
                throw new CliException(projectDir + " already exists and is not an empty directory");
        }
        else {
            if (!projectDir.mkdirs())
                throw new IOException("Unable to create destination: " + projectDir);
        }

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

        // TODO exclusions if not --for-eclipse
        String templatesUrl = releasesUrl + "com/centurylink/mdw/mdw-templates/" + getMdwVersion()
                + "/mdw-templates-" + getMdwVersion() + ".zip";
        System.out.println(" - retrieving templates: " + templatesUrl);
        File tempZip = File.createTempFile("mdw", ".zip", null);
        new Download(new URL(templatesUrl), tempZip).run();
        new Unzip(tempZip, projectDir).run();
        System.out.println(" - wrote: ");
        subst(projectDir);
    }

}
