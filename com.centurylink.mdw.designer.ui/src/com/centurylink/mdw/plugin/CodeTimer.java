/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin;

import org.eclipse.core.runtime.IStatus;

public class CodeTimer {
    private String description;
    private long startTime;
    private long stopTime;
    private boolean running;

    /**
     * Starts automatically by default.
     */
    public CodeTimer(String description) {
        this(description, true);
    }

    public CodeTimer(String description, boolean start) {
        this.description = description;

        if (start)
            start();
    }

    public void start() {
        startTime = System.currentTimeMillis();
        running = true;
    }

    public void stop() {
        stopTime = System.currentTimeMillis();
        running = false;
    }

    public long getElapsedTime() {
        long elapsed;
        if (running)
            elapsed = (System.currentTimeMillis() - startTime);
        else
            elapsed = (stopTime - startTime);

        return elapsed;
    }

    public void stopAndLog() {
        stop();
        if (MdwPlugin.getSettings().isLogTimings())
            PluginMessages.log("CodeTimer: '" + description + "': " + getElapsedTime() + " ms",
                    IStatus.INFO);
    }
}
