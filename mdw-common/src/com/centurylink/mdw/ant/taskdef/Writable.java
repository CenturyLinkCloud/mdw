/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.ant.taskdef;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Ant task for making a file writable on the file system.
 *
 *  Example:
 *  <pre>
 *  &lt;writable file="./build.xml"&gt;
 *  &lt;writable dir="./buildDir"&gt;
 *  </pre>
 */
public class Writable extends Task {
    private File file;
    public File getFile() { return file; }
    public void setFile(File f) { this.file = f; }

    private File dir;
    public File getDir() { return dir; }
    public void setDir(File d) { this.dir = d; }


    public void execute() throws BuildException {
        if ((file != null && dir != null) || (file == null && dir == null))
            throw new BuildException("Either 'file' or 'dir' must be specified");

        try {
            if (file != null) {
                if (!file.exists() || file.isDirectory())
                    throw new BuildException("File does not exist: " + file);
                log("Setting file writable: " + file);
                setWritable(file);
            }
            else if (dir != null) {
                if (!dir.exists() || !dir.isDirectory())
                    throw new BuildException("Directory does not exist: " + dir);
                log("Setting directory contents writable: " + dir);
                setWritable(dir);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            throw new BuildException(ex);
        }
    }

    protected void setWritable(File f) throws IOException {
        f.setWritable(true);
        if (f.isDirectory()) {
            for (File file : f.listFiles())
                setWritable(file);
        }
    }
}
