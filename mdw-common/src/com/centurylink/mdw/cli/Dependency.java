package com.centurylink.mdw.cli;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Download a needed dependency from Maven repo.
 */
public class Dependency implements Operation {

    private String repoUrl;
    private String path;
    private long size;

    public Dependency(String path, long size) {
        this(MAVEN_CENTRAL_URL, path, size);
    }

    public Dependency(String repoUrl, String path, long size) {
        this.repoUrl = repoUrl;
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
            new Download(new URL(repoUrl + "/" + path), depJar, size).run(progressMonitors);
        }
        return this;
    }

    /**
     * Creates libDir and throws IOException if unable.
     */
    static File getLibDir() throws IOException {
        File libDir;
        String libDirSysProp = System.getProperty("mdw.cli.lib.dir");
        if (libDirSysProp != null) {
            libDir = new File(libDirSysProp);
        }
        else {
            String mdwHome = System.getenv("MDW_HOME");
            if (mdwHome == null)
                mdwHome = System.getProperty("mdw.home");
            if (mdwHome == null)
                throw new IOException("Missing environment variable: MDW_HOME");
            File mdwDir = new File(mdwHome);
            if (!mdwDir.isDirectory() && !mdwDir.mkdirs())
                throw new IOException("MDW_HOME is not a directory: " + mdwDir.getAbsolutePath());
            libDir = new File (mdwDir + "/lib");
        }

        if (!libDir.isDirectory() && !libDir.mkdirs())
            throw new IOException("Cannot create lib dir: " + libDir.getAbsolutePath());
        return libDir;
    }
}
