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
