/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.cli;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.file.ZipHelper;

@Parameters(commandNames="init", commandDescription="Initialize an MDW project", separators="=")
public class Init {

    public Init(String project) {
        this.project = project;
    }

    Init() {
        // cli use only
    }

    @Parameter(description="<project>", required=true)
    private String project;

    @Parameter(names="--discovery-url", description="Asset Discovery URL")
    private String discoveryUrl = "https://mdw.useast.appfog.ctl.io/mdw";

    @Parameter(names="--releases-url", description="MDW Releases Maven Repo URL")
    private String releasesUrl = "http://repo.maven.apache.org/maven2";

    public void run() throws IOException {
        System.out.println("initializing " + project + "...");
        File destDir = new File(project);
        if (destDir.exists()) {
            if (!FileHelper.isEmpty(destDir))
                throw new CliException("destination: " + destDir + " already exists and is not an empty directory");
        }
        else {
            if (!destDir.mkdirs())
                throw new IOException("Unable to create destination: " + destDir);
        }

        String url = releasesUrl;
        if (!url.endsWith("/"))
            url += "/";

        System.out.println("URL: " + url);

    }

}
