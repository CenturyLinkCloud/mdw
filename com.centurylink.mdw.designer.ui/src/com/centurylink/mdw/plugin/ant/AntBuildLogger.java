/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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
