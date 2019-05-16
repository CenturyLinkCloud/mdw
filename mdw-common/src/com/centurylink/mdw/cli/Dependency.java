/*
 * Copyright (c) 2019 CenturyLink, Inc. All Rights Reserved.
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
        if (System.getProperty("mdw.studio.version") != null || System.getProperty("mdw.runtime.env") != null)
            return this; // dependencies are provided by studio and runtime

        File libDir = getLibDir();
        File depJar = new File(libDir + "/" + path.substring(path.lastIndexOf('/')));
        if (!depJar.exists()) {
            getOut().println("Downloading " + depJar + "...");
            new Download(new URL(mavenRepoUrl + "/" + path), depJar, size).run(progressMonitors);
        }
        return this;
    }

    /**
     * Creates libDir and throws IOException if unable.
     */
    static File getLibDir() throws IOException {
        String mdwHome = System.getenv("MDW_HOME");
        if (mdwHome == null)
            mdwHome = System.getProperty("mdw.home");
        if (mdwHome == null)
            throw new IOException("Missing environment variable: MDW_HOME");
        File mdwDir = new File(mdwHome);
        if (!mdwDir.isDirectory())
            throw new IOException("MDW_HOME is not a directory: " + mdwDir.getAbsolutePath());
        File libDir = new File (mdwDir + "/lib");
        if (!libDir.exists() && !libDir.mkdirs())
            throw new IOException("Cannot create lib dir: " + libDir.getAbsolutePath());
        return libDir;
    }
}
