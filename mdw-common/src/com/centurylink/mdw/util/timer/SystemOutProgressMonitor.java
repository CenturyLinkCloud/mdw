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
