/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import java.io.File;

public class FuseServer extends ServiceMixServer
{
  public static final String ID_PREFIX = "com.centurylink.server.jbossfuse";

  public String validateServerLoc()
  {
    String msg = null;
    String location = getLocation();
    if (location == null || location.isEmpty())
      msg = "";
    else
    {
      File locationFile = new File(location);
      if (!locationFile.exists() || !locationFile.isDirectory())
        msg = "Location must be an existing directory";
      else if (!new File(locationFile + "/bin/karaf.bat").exists() && !new File(locationFile + "/bin/karaf").exists())
        msg = "Location must contain bin/karaf.bat or bin/karaf";
    }
    return msg;
  }

}
