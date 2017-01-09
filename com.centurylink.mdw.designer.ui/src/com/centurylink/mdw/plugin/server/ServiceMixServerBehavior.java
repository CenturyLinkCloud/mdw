/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.RuntimeDelegate;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.actions.WorkflowElementActionHandler;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.ServerSettings.ContainerType;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ServiceMixServerBehavior extends ServerBehaviourDelegate implements MdwServerConstants
{
  public static final String REFRESH_OUTPUT_DIR_BEFORE_PUBLISH = "refreshOutputDirBeforePublish";

  public static final String DEFAULT_JAVA_OPTS = "-server -Xms512m -Xmx1024m -XX:MaxPermSize=256m -Dderby.system.home=\"%KARAF_DATA%\\derby\" -Dderby.storage.fileSyncTransactionLog=true -Dcom.sun.management.jmxremote -Dkaraf.delay.console=false";

  private ServerSettings serverSettings;

  public void start(boolean debug, IProgressMonitor monitor) throws CoreException
  {
    serverSettings = getServerSettings();
    if (serverSettings == null)
    {
      String msg = "Unable to find server settings for: " + getServer().getName()
          + ".\nCheck Runtime Environment and Server configurations.";
      showError(msg, "Launch " + getServer().getName());
      return;
    }

    if (debug)
    {
      // debugging only for this run
      serverSettings.setDebug(true);
      if (serverSettings.getDebugPort() == 0)
        serverSettings.setDebugPort(8500);
    }

    if (getServer().shouldPublish() && ServerCore.isAutoPublishing())
      getServer().publish(IServer.PUBLISH_CLEAN, monitor);

//    if (ServerRunner.isServerRunning())
//      return;

    setServerRestartState(false);
    setServerState(IServer.STATE_STARTING);
    setMode(debug ? "debug" : "run");

    MdwPlugin.getDisplay().asyncExec(new Runnable()
    {
      public void run()
      {
        try
        {
          WorkflowElementActionHandler actionHandler = new WorkflowElementActionHandler();
          actionHandler.run(serverSettings);
        }
        catch (Exception ex)
        {
          PluginMessages.uiError(ex, "Server Launch");
          setServerState(IServer.STATE_STOPPED);
          return;
        }
      }
    });
  }

  @Override
  public void stop(boolean force)
  {
    int state = getServer().getServerState();
    // stopped or stopping, no need to run stop command again
    if (state == IServer.STATE_STOPPED || state == IServer.STATE_STOPPING)
      return;

    setServerState(IServer.STATE_STOPPING);

    serverSettings = getServerSettings();
    if (serverSettings == null)
    {
      showError("Launch " + getServer().getName(), "Unable to find server settings for: " + getServer().getName());
      return;
    }

    MdwPlugin.getDisplay().asyncExec(new Runnable()
    {
      public void run()
      {
        try
        {
          WorkflowElementActionHandler actionHandler = new WorkflowElementActionHandler();
          actionHandler.stop(serverSettings);
        }
        catch (Exception ex)
        {
          PluginMessages.uiError(ex, "Server Launch");
        }
        setServerState(IServer.STATE_STOPPED);
      }
    });
  }

  @Override
  public IStatus canRestart(String mode)
  {
    return new Status(IStatus.ERROR, MdwPlugin.PLUGIN_ID, 0, "No restarts", null);
  }

  public void setPublishState(int publishState)
  {
    setServerPublishState(publishState);
  }

  @Override
  public IModuleResourceDelta[] getPublishedResourceDelta(IModule[] module)
  {
    return super.getPublishedResourceDelta(module);
  }

  public IModuleResource[] getResources(IModule[] module)
  {
    return super.getResources(module);
  }

  protected void showError(final String message, final String title)
  {
    MdwPlugin.getDisplay().asyncExec(new Runnable()
    {
      public void run()
      {
        PluginMessages.log(message);
        PluginMessages.uiError(message, title);
      }
    });
  }

  public boolean isRefreshOutputDirectoryBeforePublish()
  {
    return getServer().getAttribute(REFRESH_OUTPUT_DIR_BEFORE_PUBLISH, "true").equalsIgnoreCase("true");
  }

  ServerSettings getServerSettings()
  {
    serverSettings = null;
    String serverLoc = getServer().getAttribute(ServiceMixServer.LOCATION, "");
    ServiceMixRuntime runtime = (ServiceMixRuntime) getServer().getRuntime().loadAdapter(RuntimeDelegate.class, null);
    if (!serverLoc.isEmpty() && runtime != null && runtime.getLocation() != null && !runtime.getLocation().isEmpty())
    {
      serverSettings = new ServerSettings(getProject());
      serverSettings.setContainerType(ContainerType.ServiceMix);
      serverSettings.setHome(runtime.getLocation().toString());
      serverSettings.setJdkHome(runtime.getJavaHome());
      serverSettings.setServerName(getServer().getName());
      serverSettings.setServerLoc(serverLoc);
      serverSettings.setHost(getServer().getHost());
      serverSettings.setPort(getServer().getAttribute(ServiceMixServer.SERVER_PORT, 0));
      serverSettings.setCommandPort(getServer().getAttribute(ServiceMixServer.SSH_PORT, 0));
      serverSettings.setUser(getServer().getAttribute(ServiceMixServer.USER, ""));
      serverSettings.setPassword(getServer().getAttribute(ServiceMixServer.PASSWORD, ""));
      serverSettings.setJavaOptions(getServer().getAttribute(JAVA_OPTIONS, DEFAULT_JAVA_OPTS));
      serverSettings.setDebug(getServer().getAttribute(DEBUG_MODE, DEFAULT_DEBUG_MODE));
      serverSettings.setDebugPort(getServer().getAttribute(DEBUG_PORT, DEFAULT_DEBUG_PORT));
      serverSettings.setSuspend(getServer().getAttribute(DEBUG_SUSPEND, DEFAULT_DEBUG_SUSPEND));
    }

    if (serverSettings == null)
    {
      // compatibility for old servers
      WorkflowProject workflowProject = getProject();
      if (workflowProject == null)
        return null;

      serverSettings = workflowProject.getServerSettings();
      serverSettings.setServerName(getServer().getName());
    }
    return serverSettings;
  }

  WorkflowProject getProject()
  {
    for (IModule module : getServer().getModules())
    {
      IProject project = module.getProject();
      if (project != null)
      {
        WorkflowProject wfProj = WorkflowProjectManager.getInstance().getWorkflowProject(project);
        if (wfProj != null)
          return wfProj;
      }
    }
    return null;
  }
}
