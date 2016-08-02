/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

import com.centurylink.mdw.plugin.PluginMessages;

public class AutomatedTestTabGroup extends AbstractLaunchConfigurationTabGroup
{
  private ILaunchConfigurationDialog dialog;
  private FunctionTestLaunchTab functionTestTab;
  private LoadTestLaunchTab loadTestTab;
  
  
  public void createTabs(ILaunchConfigurationDialog dialog, String mode)
  {
    this.dialog = dialog;
    functionTestTab = new FunctionTestLaunchTab();
    loadTestTab = new LoadTestLaunchTab();
    
    ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[]
    {
        functionTestTab, 
        loadTestTab, 
        new EnvironmentTab(),
        new CommonTab()
    };
    setTabs(tabs);
  }
  
  @Override
  public void initializeFrom(ILaunchConfiguration configuration)
  {
    super.initializeFrom(configuration);
    
    try
    {
      if (configuration.getAttribute(AutomatedTestLaunchConfiguration.IS_LOAD_TEST, false))
        dialog.setActiveTab(loadTestTab);
    }
    catch (CoreException ex)
    {
      PluginMessages.log(ex);
    }
  }
  

}
