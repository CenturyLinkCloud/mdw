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
package com.centurylink.mdw.plugin.designer;

import org.eclipse.core.runtime.IProgressMonitor;

import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;

public class SwtProgressMonitor implements ProgressMonitor {
    private IProgressMonitor wrappedMonitor;

    public SwtProgressMonitor(IProgressMonitor wrappedMonitor) {
        this.wrappedMonitor = wrappedMonitor;
    }

    IProgressMonitor getWrappedMonitor() {
        return wrappedMonitor;
    }

    public void start(String taskName) {
        if (wrappedMonitor != null)
            wrappedMonitor.beginTask(taskName, 100);
    }

    public void progress(int percentagePoints) {
        if (wrappedMonitor != null)
            wrappedMonitor.worked(percentagePoints);
    }

    public void subTask(String subTaskName) {
        if (wrappedMonitor != null)
            wrappedMonitor.subTask(subTaskName);
    }

    public void done() {
        if (wrappedMonitor != null)
            wrappedMonitor.done();
    }

    public boolean isCanceled() {
        return wrappedMonitor == null ? false : wrappedMonitor.isCanceled();
    }

    public void setCanceled(boolean canceled) {
        if (wrappedMonitor != null)
            wrappedMonitor.setCanceled(canceled);
    }
}
