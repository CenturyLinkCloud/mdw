/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaConnectTab;

public class JavaDebugTabGroup extends AbstractLaunchConfigurationTabGroup
{
  public void createTabs(ILaunchConfigurationDialog dialog, String mode)
  {
    setTabs(new ILaunchConfigurationTab[] { new JavaConnectTab() });
  }
}
