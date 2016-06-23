/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project;

import org.eclipse.wst.server.core.IRuntime;

public class FuseSettingsPage extends ServiceMixSettingsPage
{
  public static final String PAGE_TITLE = "Fuse Settings";
  public static final String DEFAULT_USER = "karaf";

  public String getPageTitle() { return PAGE_TITLE; }

  public String getServerName() { return "JBoss Fuse"; }

  public String getDefaultServerUser() { return DEFAULT_USER; }


  public boolean checkForAppropriateRuntime(IRuntime runtime)
  {
    String vendor = runtime.getRuntimeType().getVendor();
    String name = runtime.getRuntimeType().getName();
    return vendor.equals("JBoss") && name.startsWith("Fuse");
  }

  protected String serverHomeSpecializedCheck(String serverHome)
  {
    if (getUseRootInstanceCheckbox().getSelection())
    {
      getServerSettings().setServerLoc(serverHome);
      if (!serverLocTextField.getText().equals(getServerSettings().getServerLoc()))
      serverLocTextField.setText(getServerSettings().getServerLoc());
    }

    String msg = null;
    if (serverHome != null && serverHome.length() != 0 && !(checkFile(serverHome + "/bin/fuse.bat") || checkFile(serverHome + "/bin/fuse")))
      msg = "Fuse Home must contain bin/fuse.bat or bin/fuse.sh";

    return msg;
  }

  protected String serverLocSpecializedCheck(String serverLoc)
  {
    String msg = null;
    if (serverLoc != null && serverLoc.length() != 0 && !(checkFile(serverLoc + "/bin/karaf.bat") || checkFile(serverLoc + "/bin/karaf")))
      msg = "Instance Location must contain bin/karaf.bat or bin/karaf";

    return msg;
  }

}
