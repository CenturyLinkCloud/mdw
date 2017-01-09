/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaConnectTab;

public class ActivityLaunchTabGroup extends AbstractLaunchConfigurationTabGroup
{
  private ActivityLaunchMainTab mainTab;
  private ProcessVariablesTab variablesTab;
  private JavaConnectTab connectTab;
  private CommonTab commonTab;
  
  public void createTabs(ILaunchConfigurationDialog dialog, String mode)
  {
    mainTab = new ActivityLaunchMainTab();
    variablesTab = new ProcessVariablesTab();
    commonTab = new CommonTab();
    if (mode.equals(ILaunchManager.DEBUG_MODE))
      connectTab = new JavaConnectTab();
    
    ILaunchConfigurationTab[] tabs;
    if (mode.equals(ILaunchManager.DEBUG_MODE))
    {
      tabs = new ILaunchConfigurationTab[]
      {
        mainTab, 
        variablesTab,
        connectTab,
        commonTab
      };
    }
    else
    {
      tabs = new ILaunchConfigurationTab[]
      {
        mainTab, 
        variablesTab,
        commonTab
      };
    }
    
    setTabs(tabs);
  }
}