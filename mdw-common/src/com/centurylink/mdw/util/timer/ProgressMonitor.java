/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util.timer;

public interface ProgressMonitor {

    public void start(String taskName);
    
    public void progress(int points);

    public void subTask(String subTaskName);
    
    public void done();
    
    public boolean isCanceled();
    public void setCanceled(boolean canceled);
}
