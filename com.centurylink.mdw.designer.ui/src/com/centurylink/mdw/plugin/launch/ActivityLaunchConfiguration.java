/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;

public class ActivityLaunchConfiguration extends ProcessLaunchConfiguration
{
  public static final String ACTIVITY_ID = "activityId";

  private Long activityId;
  
  public void launch(ILaunchConfiguration launchConfig, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException
  {
    String activityIdStr = launchConfig.getAttribute(ACTIVITY_ID, "");
    try
    {
      activityId = Long.valueOf(activityIdStr);
    }
    catch (NumberFormatException ex)
    {
      showError("Can't locate activity ID: '" + activityIdStr + "'.", "Activity Launch", null);
      return;
    }
    
    super.launch(launchConfig, mode, launch, monitor);
  }
  
  @Override
  protected void launchProcess(WorkflowProcess process, String masterRequestId, String owner, Long ownerId, boolean synchronous, String responseVarName, Map<String,String> parameters, Long activityId, boolean showLogs, int logWatchPort, boolean liveView)
  {
    super.launchProcess(process, masterRequestId, owner, ownerId, synchronous, responseVarName, parameters, this.activityId, showLogs, logWatchPort, liveView);
  }
}
