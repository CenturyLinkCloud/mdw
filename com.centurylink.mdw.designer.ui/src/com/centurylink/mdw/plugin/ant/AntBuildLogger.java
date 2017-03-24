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
package com.centurylink.mdw.plugin.ant;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.eclipse.core.runtime.IProgressMonitor;

import com.centurylink.mdw.plugin.PluginMessages;

/**
 * Logger for plug-in ant build output. Uses an IProgressMonitor for build
 * status output. If the progress monitor is null, uses the log file only.
 */
public class AntBuildLogger extends DefaultLogger {
    private IProgressMonitor monitor;

    /**
     * Constructor takes a progress monitor.
     *
     * @param monitor
     *            - uses 5 units per ant task
     */
    public AntBuildLogger(IProgressMonitor monitor) {
        this.monitor = monitor;
    }

    public void buildStarted(BuildEvent event) {
        if (monitor != null)
            monitor.worked(5);

        PluginMessages.log("Starting Ant Build.");
    }

    public void buildFinished(BuildEvent event) {
        Throwable error = event.getException();

        if (error == null) {
            PluginMessages.log("Ant Build Successful.");
        }
        else {
            PluginMessages.log("Ant Build Failed.");
            PluginMessages.log(error);
        }
    }

    public void messageLogged(BuildEvent event) {
        if (event.getPriority() < Project.MSG_INFO) {
            PluginMessages.log("Ant Build Error: " + event.getMessage());
        }
    }

    public void targetStarted(BuildEvent event) {
        if (monitor != null)
            monitor.worked(5);
    }

    public void targetFinished(BuildEvent event) {
        if (monitor != null)
            monitor.worked(5);
    }

}
