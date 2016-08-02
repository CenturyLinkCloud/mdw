/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;

/**
 * This is for standalone (non-MDW) cucumber tests.
 */
public class CucumberTabGroup extends AbstractLaunchConfigurationTabGroup
{
  private CucumberLaunchTab cucumberTab;

  public void createTabs(ILaunchConfigurationDialog dialog, String mode)
  {
    cucumberTab = new CucumberLaunchTab();

    ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[]
    {
      cucumberTab,
      new JavaArgumentsTab(),
      new JavaJRETab(),
      new JavaClasspathTab(),
      new CommonTab()
    };
    setTabs(tabs);
  }
}
