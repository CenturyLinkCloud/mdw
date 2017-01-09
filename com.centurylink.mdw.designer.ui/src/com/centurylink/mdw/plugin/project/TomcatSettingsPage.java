/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IRuntime;

import com.centurylink.mdw.plugin.project.model.ServerSettings;

public class TomcatSettingsPage extends ServerSettingsPage
{
  public static final String PAGE_TITLE = "Tomcat Settings";
  public static final int DEFAULT_PORT = 8080;
  public static final String DEFAULT_USER = "tomcat";
  
  public TomcatSettingsPage()
  {
    setTitle(PAGE_TITLE);
    setDescription("Enter your Tomcat server information.\n"
        + "This will be written to your environment properties file.");
  }
  
  public String getServerName() { return "Tomcat"; }
  public int getDefaultServerPort() { return DEFAULT_PORT; }
  public String getDefaultServerUser() { return DEFAULT_USER; }  
  
  public boolean checkForAppropriateRuntime(IRuntime runtime)
  {
    String vendor = runtime.getRuntimeType().getVendor();
    String version = runtime.getRuntimeType().getVersion();
    return (vendor.equals("Apache") || vendor.equals("Apache")) && version.startsWith("6");    
  }
  
  @Override
  protected void createServerLocControls(Composite parent, int ncol)
  {
    // same as server home
  }
  
  protected String serverHomeSpecializedCheck(String serverHome)
  {
    if (serverHome != null && serverHome.length() != 0 && !checkFile(serverHome + "/lib/catalina.jar"))
      return "Apache Tomcat Home must contain lib/catalina.jar";
    else
      return null;
  }
  
  protected String serverLocSpecializedCheck(String serverLoc)
  {
    ServerSettings serverSettings = getProject().getServerSettings();
    serverSettings.setServerLoc(serverSettings.getHome());
    return null;
  }
  
}
