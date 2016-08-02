/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.Activity;

public class ActivityLaunchShortcut extends ProcessLaunchShortcut
{
  public static final String ACTIVITY_LAUNCH_CONFIG_TYPE = "com.centurylink.mdw.plugin.launch.Activity";
  
  public void launch(ISelection selection, String mode)
  {
    Object firstElement = ((StructuredSelection)selection).getFirstElement();
    if (firstElement instanceof Activity)
    {
      Activity activity = (Activity) firstElement;
      try
      {
        boolean prevEnablement = disableBuildBeforeLaunch();
        performLaunch(activity, mode);
        setBuildBeforeLaunch(prevEnablement);
      }
      catch (Exception ex)
      {
        PluginMessages.uiError(ex, "Activity Launch", activity.getProject());
      }
    }
  }

  private void performLaunch(Activity activity, String mode) throws InterruptedException, CoreException
  {
    boolean debug = mode.equals(ILaunchManager.DEBUG_MODE);
    ILaunchConfigurationWorkingCopy workingCopy = createLaunchConfiguration(ACTIVITY_LAUNCH_CONFIG_TYPE, activity.getProcess(), debug);
    workingCopy.setAttribute(ActivityLaunchConfiguration.ACTIVITY_ID, activity.getId().toString());
    ILaunchConfiguration config = findExistingLaunchConfiguration(workingCopy, activity.getProject(), debug);
    if (config == null)
    {
      // no existing found - create a new one
      config = workingCopy.doSave();
    }
    
    Shell shell = MdwPlugin.getActiveWorkbenchWindow().getShell();
    IStructuredSelection selection = new StructuredSelection(config);
    String groupId = "com.centurylink.mdw.plugin.launch.group.mdw.activity";
    if (mode.equals(ILaunchManager.DEBUG_MODE))
      groupId += ".debug";
    
    DebugUITools.openLaunchConfigurationDialogOnGroup(shell, selection, groupId);
  }
  
  
}
