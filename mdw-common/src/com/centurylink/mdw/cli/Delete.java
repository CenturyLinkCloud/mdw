package com.centurylink.mdw.cli;

import java.io.File;
import java.io.IOException;

/**
 * Recursively delete.
 */
public class Delete implements Operation {

    private File file;
    private boolean includeSubs;

    public Delete(File file) {
        this(file, false);
    }

    public Delete(File file, boolean includeSubpackages) {
        this.file = file;
        this.includeSubs = includeSubpackages;
    }

    public Delete run(ProgressMonitor... progressMonitors) throws IOException {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                if (includeSubs || !new File(child + "/.mdw/package.json").isFile() || !new File(child + "/.mdw/package.yaml").isFile())
                    new Delete(child, includeSubs).run();
            }
        }
        if (!file.delete())
            throw new IOException("Failed to delete: " + file.getAbsolutePath());

        return this;
    }
}
