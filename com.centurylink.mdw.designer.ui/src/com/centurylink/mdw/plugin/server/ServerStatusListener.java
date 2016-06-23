/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

public interface ServerStatusListener
{
  public static final String SERVER_STATUS_RUNNING = "running";
  public static final String SERVER_STATUS_STOPPED = "stopped";
  public static final String SERVER_STATUS_ERRORED = "errored";
  public static final String SERVER_STATUS_WAIT = "wait";

  public void statusChanged(String newStatus);
}
