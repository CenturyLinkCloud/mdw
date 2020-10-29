package com.centurylink.mdw.container.plugin;

public interface CommonThreadPoolMXBean {

    boolean hasAvailableThread(String workerName);

    boolean isPaused();

    String currentStatus();

    int getCurrentThreadPoolSize();

    int getCoreThreadPoolSize();

    int getMaxThreadPoolSize();

    int getActiveThreadCount();

    int getCurrentQueueSize();

    long getTaskCount();

    long getCompletedTaskCount();

    String workerInfo();

    String defaultWorkerInfo();
}
