package com.centurylink.mdw.git;

import org.eclipse.jgit.lib.ProgressMonitor;

public interface GitProgressMonitor extends ProgressMonitor {
    void done();
    void error(Throwable t);
}
