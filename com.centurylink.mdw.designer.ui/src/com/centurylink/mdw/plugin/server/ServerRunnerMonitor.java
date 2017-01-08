/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.model.ServerDelegate;

import com.centurylink.mdw.plugin.project.model.ServerSettings;

@SuppressWarnings("restriction")
public class ServerRunnerMonitor implements ServerStatusListener
{
  private ServerSettings serverSettings;
  private ServerStatusChecker statusCheck;

  private Server server;

  public ServerRunnerMonitor(ServerSettings serverSettings)
  {
    this.serverSettings = serverSettings;
    statusCheck = new ServerStatusChecker(serverSettings);
  }

  public void startup()
  {
    for (IServer server : ServerCore.getServers())
    {
      if (server.getRuntime() != null && server.getRuntime().getRuntimeType() != null
          && server.getRuntime().getRuntimeType().getId().startsWith("com.centurylink.server.runtime"))
      {
        ServiceMixServer smxServer = (ServiceMixServer)server.loadAdapter(ServerDelegate.class, null);
        if (smxServer.getName().equals(serverSettings.getServerName()))
        {
          this.server = (Server)server;
          statusCheck.addStatusListener(this);
          statusCheck.start();
        }
      }
    }
  }

  public void statusChanged(String status)
  {
    if (status.equals(ServerStatusListener.SERVER_STATUS_RUNNING))
      server.setServerState(IServer.STATE_STARTED);
    else if (server.getServerState() != IServer.STATE_STARTING
        && (status.equals(ServerStatusListener.SERVER_STATUS_STOPPED) || status.equals(ServerStatusListener.SERVER_STATUS_ERRORED)))
      server.setServerState(IServer.STATE_STOPPED);
    else if (status.equals(ServerStatusListener.SERVER_STATUS_WAIT))
      server.setServerState(IServer.STATE_STARTING);

    if (server.getServerState() == IServer.STATE_STOPPED)
    {
      if (statusCheck != null)
        statusCheck.stop();
    }
  }
}
