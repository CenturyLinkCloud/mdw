/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.cli;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Download a needed dependency from Maven repo.
 */
public class Dependency implements Operation {

    private String mavenRepoUrl;
    private String path;
    private long size;

    public Dependency(String mavenRepoUrl, String path, long size) {
        this.mavenRepoUrl = mavenRepoUrl;
        this.path = path;
        this.size = size;
    }

    @Override
    public Dependency run(ProgressMonitor... progressMonitors) throws IOException {
        String mdwHome = System.getenv("MDW_HOME");
        if (mdwHome == null)
            throw new IOException("Missing environment variable: MDW_HOME");
        File mdwDir = new File(mdwHome);
        if (!mdwDir.isDirectory())
            throw new IOException("MDW_HOME is not a directory: " + mdwDir.getAbsolutePath());
        File libDir = new File (mdwDir + "/lib");
        if (!libDir.exists() && !libDir.mkdirs())
            throw new IOException("Cannot create lib dir: " + libDir.getAbsolutePath());

        File depJar = new File(libDir + "/" + path.substring(path.lastIndexOf('/')));
        if (!depJar.exists()) {
            System.out.println("Downloading " + depJar + "...");
            new Download(new URL(mavenRepoUrl + "/" + path), depJar, size).run(progressMonitors);
        }
        return this;
    }
}
