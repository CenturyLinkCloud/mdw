/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util.timer;

/**
 * Logs task statuses only.  Does not indicate actual progress.
 */
public class SystemOutProgressMonitor implements ProgressMonitor {

    private String taskName;

    public void start(String taskName) {
        this.taskName = taskName;
        System.out.println(taskName + "...");
    }

    public void progress(int points) {
        // no progress indication
    }

    public void subTask(String subTaskName) {
        System.out.println("   " + subTaskName);
    }

    public void done() {
        System.out.println(taskName + " done.");

    }

    private boolean canceled;
    public boolean isCanceled() {
        return canceled;
    }
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

}
