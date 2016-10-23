/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util.timer;

import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Logs task statuses only.  Does not indicate actual progress.
 */
public class LoggerProgressMonitor implements ProgressMonitor {

    private StandardLogger logger;
    public LoggerProgressMonitor(StandardLogger logger) {
        this.logger = logger;
    }

    private String taskName;
    public void start(String taskName) {
        this.taskName = taskName;
        logger.info(taskName + "...");
    }

    public void progress(int points) {
        // no progress indication
    }

    public void subTask(String subTaskName) {
        logger.info("   " + subTaskName);
    }

    public void done() {
        logger.info(taskName + " done.");
    }

    private boolean canceled;
    public boolean isCanceled() {
        return canceled;
    }
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

}
