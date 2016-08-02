/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IRuntime;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.project.model.ServerSettings;

/**
 * WebLogic Settings page of the MDW workflow project wizard.
 */
public class WebLogicSettingsPage extends ServerSettingsPage
{
  public static final String PAGE_TITLE = "WebLogic Settings";
  public static final String DEFAULT_USER = "weblogic";
  public static final int DEFAULT_PORT = 7001;
  
  private Text webLogicDomainTextField;
  private Text webLogicServerTextField;
  
  /**
   * Constructor.
   */
  public WebLogicSettingsPage()
  {
    setTitle(PAGE_TITLE);
    setDescription("Enter your WebLogic Server information.\n"
        + "This will be written to your environment properties file.");
  }
  
  public String getServerName() { return "WebLogic"; }
  public int getDefaultServerPort() { return DEFAULT_PORT; }
  public String getDefaultServerUser() { return DEFAULT_USER; }
  
  public boolean checkForAppropriateRuntime(IRuntime runtime)
  {
    if (runtime.getRuntimeType() != null)
    {
      String vendor = runtime.getRuntimeType().getVendor();
      String version = runtime.getRuntimeType().getVersion();
      return ("BEA Systems, Inc.".equals(vendor) || "Oracle".equals(vendor)) && (version != null && version.startsWith("10"));
    }
    return false;
  }  
  
  @Override
  protected void createAdditionalServerInfoControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Domain Name:");
    webLogicDomainTextField = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 200;
    gd.horizontalSpan = ncol - 1;
    webLogicDomainTextField.setLayoutData(gd);

    new Label(parent, SWT.NONE).setText("Server Name:");
    webLogicServerTextField = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 200;
    gd.horizontalSpan = ncol - 1;
    webLogicServerTextField.setLayoutData(gd);    
  }

  protected String serverHomeSpecializedCheck(String serverHome)
  {
    if (serverHome != null && serverHome.length() != 0 && !checkFile(serverHome + "/server/lib/weblogic.jar"))
      return "WebLogic Home must contain server/lib/weblogic.jar";
    else
      return null;
  }
  
  protected String serverLocSpecializedCheck(String serverLoc)
  {
    if (serverLoc != null && serverLoc.length() != 0 && !checkDir(serverLoc + "/logs"))
    {
      File serverLocDir = new File(serverLoc);
      if (serverLocDir.exists() && serverLocDir.isDirectory())
      {
        // try to create the logs dir
        File logsDir = new File(serverLocDir + "/logs");
        if (!logsDir.mkdir())
          return "WebLogic Domain Dir must contain a logs directory"; 
      }
    }
    
    return null;
  }

  protected String getServerLocationLabel()
  {
    return "Domain Location";
  }

  /**
   * sets the completed field on the wizard class when all the information 
   * on the page is entered
   */
  public boolean isPageComplete()
  {
    if (getStatuses() != null)
      return false;
    
    ServerSettings wlSettings = getProject().getServerSettings();
    return super.isPageComplete()
      && checkStringNoWhitespace(wlSettings.getUser())
      && checkStringNoWhitespace(wlSettings.getPassword());
  }  

  protected void parseServerAdditionalInfo(String serverLoc)
  {    
    ServerSettings serverSettings = getProject().getServerSettings();
    try
    {
      getConfigurator().parseServerAdditionalInfo();
    }
    catch (Exception ex)
    {
      webLogicDomainTextField.setText("Can't parse config.xml");
      PluginMessages.log(ex);
    }
    
    if (serverSettings.getDomainName() != null)
      webLogicDomainTextField.setText(serverSettings.getDomainName());
    if (serverSettings.getServerName() != null)
      webLogicServerTextField.setText(serverSettings.getServerName());
  }
}
