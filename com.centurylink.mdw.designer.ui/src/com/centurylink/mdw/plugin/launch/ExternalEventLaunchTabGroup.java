/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

import com.centurylink.mdw.plugin.launch.ExternalEventTab.Mode;

public class ExternalEventLaunchTabGroup extends AbstractLaunchConfigurationTabGroup
{
  private ExternalEventTab externalEventTab;
  private CommonTab commonTab;
  
  public void createTabs(ILaunchConfigurationDialog dialog, String mode)
  {
    externalEventTab = new ExternalEventTab(Mode.ExternalEvent);
    commonTab = new CommonTab();
    
    ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[]
    {
      externalEventTab,
      commonTab
    };
    
    setTabs(tabs);
  }
}
