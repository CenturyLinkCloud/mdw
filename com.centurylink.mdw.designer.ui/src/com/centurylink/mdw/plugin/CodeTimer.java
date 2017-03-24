/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
