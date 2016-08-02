/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.common.utilities.HttpHelper;

public class ServerStatusChecker implements Runnable
{
  private Thread statusCheckThread;

  private ServerSettings serverSettings;
  public ServerSettings getServerSettings() { return serverSettings; }

  private List<ServerStatusListener> statusListeners = new ArrayList<ServerStatusListener>();

  public void addStatusListener(ServerStatusListener listener)
  {
    if (!statusListeners.contains(listener))
      statusListeners.add(listener);
  }
  public void removeStatusListener(ServerStatusListener listener)
  {
    statusListeners.remove(listener);
  }

  public ServerStatusChecker(ServerSettings settings)
  {
    this.serverSettings = settings;
  }

  private boolean stopping;

  public void run()
  {
    updateServerStatus(ServerStatusListener.SERVER_STATUS_WAIT);
    while (!stopping)
    {
      try
      {
        HttpHelper httpHelper = null;
        try
        {
          URL url = new URL(serverSettings.getConsoleUrl());
          httpHelper = new HttpHelper(url);
          httpHelper.setConnectTimeout(MdwPlugin.getSettings().getHttpConnectTimeout());
          httpHelper.setReadTimeout(MdwPlugin.getSettings().getHttpReadTimeout());
          if (httpHelper.get() == null)
            updateServerStatus(ServerStatusListener.SERVER_STATUS_STOPPED);
          else
            updateServerStatus(ServerStatusListener.SERVER_STATUS_RUNNING);
        }
        catch (FileNotFoundException ex)
        {
          if ((serverSettings.isServiceMix() || serverSettings.isFuse()) && httpHelper.getResponseCode() == 404)
            updateServerStatus(ServerStatusListener.SERVER_STATUS_RUNNING);  // expected
          else
            updateServerStatus(ServerStatusListener.SERVER_STATUS_STOPPED);
        }
        catch (IOException ex)
        {
          updateServerStatus(ServerStatusListener.SERVER_STATUS_STOPPED);
        }
        catch (Exception ex)
        {
          updateServerStatus(ServerStatusListener.SERVER_STATUS_ERRORED);
          PluginMessages.log(ex);
        }
        Thread.sleep(6000);
      }
      catch (InterruptedException ex)
      {
        break;
      }
    }
  }

  public void start()
  {
    stopping = false;
    statusCheckThread = new Thread(this);
    statusCheckThread.start();
  }

  public void stop()
  {
    stopping = true;
    statusCheckThread = null;
  }

  private String previousStatus;
  private void updateServerStatus(String status)
  {
    if (!status.equals(previousStatus))
    {
      for (ServerStatusListener listener : statusListeners)
        listener.statusChanged(status);
      previousStatus = status;
    }
  }
}
