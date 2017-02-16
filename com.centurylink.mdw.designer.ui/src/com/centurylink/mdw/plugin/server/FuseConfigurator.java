/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class FuseConfigurator extends ServiceMixConfigurator {
    public FuseConfigurator(ServerSettings serverSettings) {
        super(serverSettings);
    }

    @SuppressWarnings("restriction")
    public void doDeploy(Shell shell) {
        org.eclipse.wst.server.core.internal.Server matchingServer = null;
        for (IServer server : ServerCore.getServers()) {
            if (server.getRuntime() != null && server.getRuntime().getRuntimeType() != null
                    && server.getRuntime().getRuntimeType().getId()
                            .startsWith("com.centurylink.server.runtime.jbossfuse")) {
                org.eclipse.wst.server.core.internal.Server smixServer = (org.eclipse.wst.server.core.internal.Server) server;
                if (smixServer.getAllModules().size() > 0) {
                    String wfProjectName = smixServer.getAllModules().get(0)[0].getName();
                    WorkflowProject serverWfp = WorkflowProjectManager.getInstance()
                            .getWorkflowProject(wfProjectName);
                    if (serverWfp != null && serverWfp.equals(getServerSettings().getProject()))
                        matchingServer = smixServer;
                }
            }
        }

        if (matchingServer != null) {
            matchingServer.publish(IServer.PUBLISH_INCREMENTAL, new NullProgressMonitor());
        }
        else {
            MessageDialog.openError(shell, "Server Deploy",
                    "Could not find a Fues server with a module matching "
                            + getServerSettings().getProject());
        }
    }

    @Override
    public String getStartCommand() {
        String cmd = (FILE_SEP.equals("/") ? "fuse" : "fuse.bat");
        if (!new File(getCommandDir() + FILE_SEP + cmd).exists())
            cmd = (FILE_SEP.equals("/") ? "karaf" : "karaf.bat");

        String fullCmd = getCommandDir() + FILE_SEP + cmd;

        if (cmd.equals("karaf.bat") && !new File(fullCmd).exists()) {
            // initially for Fuse karaf.bat may need to be created
            StringBuffer karafBat = new StringBuffer();
            karafBat.append("@ECHO OFF").append("\r\n");
            karafBat.append("SETLOCAL").append("\r\n");
            karafBat.append("SET KARAF_HOME=")
                    .append(getServerSettings().getHome().replace('/', '\\')).append("\r\n");
            karafBat.append("SET KARAF_BASE=")
                    .append(getServerSettings().getServerLoc().replace('/', '\\')).append("\r\n");
            karafBat.append("%KARAF_HOME%\\bin\\karaf.bat %*").append("\r\n");
            try {
                PluginUtil.writeFile(new File(fullCmd), karafBat.toString().getBytes());
            }
            catch (IOException ex) {
                PluginMessages.uiError(ex, "Create Start Cmd",
                        this.getServerSettings().getProject());
            }
        }

        return fullCmd;
    }

    @Override
    public String getStopCommand() {
        String cmd = (FILE_SEP.equals("/") ? "karaf stop" : "karaf.bat stop");
        return getCommandDir() + FILE_SEP + cmd;
    }
}
