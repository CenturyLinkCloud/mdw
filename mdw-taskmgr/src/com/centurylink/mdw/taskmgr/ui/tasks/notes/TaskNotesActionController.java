/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.notes;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.notes.NotesActionController;
import com.centurylink.mdw.web.ui.UIException;

public class TaskNotesActionController extends NotesActionController
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  
  public String getManagedBeanName()
  {
    return "taskNotesItem";
  }

  public String getNavigationOutcome()
  {
    return "go_taskNotes";
  }
  
  public Long getOwnerId()
  {
    long instanceId = 0;    
    try
    {
      instanceId = Long.parseLong(DetailManager.getInstance().getTaskDetail().getInstanceId()); 
    }
    catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
    return new Long(instanceId);
  }
  
  public String getActionControllerName()
  {
    return "taskNotesActionController";
  }

  
}
