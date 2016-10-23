/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.container.plugin;

public interface CommonThreadPoolMXBean {

    public void start();

    public void stop();

    public boolean hasAvailableThread(String workerName);

    public void pause();

    public void resume();

    public boolean isPaused();

    public String currentStatus();

    public int getCurrentThreadPoolSize();

    public int getCoreThreadPoolSize();

    public int getMaxThreadPoolSize();

    public int getActiveThreadCount();

    public int getCurrentQueueSize();

    public long getTaskCount();

    public long getCompletedTaskCount();

    public String workerInfo();

    public String defaultWorkerInfo();

}
