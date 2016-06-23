/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.plugin.server.FuseServer;
import com.centurylink.mdw.plugin.server.FuseServerBehavior;
import com.centurylink.mdw.plugin.server.ServiceMixServer;
import com.centurylink.mdw.plugin.server.ServiceMixServerBehavior;

public class ServerRunnerLaunchConfiguration extends LaunchConfigurationDelegate
{
  public static final String WORKFLOW_PROJECT = "workflowProject";

  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException
  {
    IServer server = ServerUtil.getServer(configuration);

    if (server == null)
    {
      showError("Launch configuration could not find server", "Server Launch", null);
      return;
    }

    ServiceMixServerBehavior serverBehave = null;
    if (server.getServerType().getId().startsWith(ServiceMixServer.ID_PREFIX))
      serverBehave = (ServiceMixServerBehavior) server.loadAdapter(ServiceMixServerBehavior.class, null);
    else if (server.getServerType().getId().startsWith(FuseServer.ID_PREFIX))
      serverBehave = (FuseServerBehavior) server.loadAdapter(FuseServerBehavior.class, null);

    if (serverBehave == null)
    {
      showError("ServerBehaviorDelagate could not be loaded", "Server Launch", null);
      return;
    }

    if (!"true".equals(configuration.getAttribute("STOP", "false")))
      serverBehave.start(mode.equals(ILaunchManager.DEBUG_MODE), monitor);
  }

  protected void showError(final String message, final String title, final WorkflowProject workflowProject)
  {
    MdwPlugin.getDisplay().asyncExec(new Runnable()
    {
      public void run()
      {
        PluginMessages.uiError(message, title, workflowProject);
      }
    });
  }

}
