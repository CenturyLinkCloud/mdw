/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.taskmgr.ui.Lister;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;

public class TaskNames implements Lister
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();      

  public List<SelectItem> list()
  {
    List<String> taskNames = new ArrayList<String>();
    taskNames.add(" ");

    List<SelectItem> taskNameSelects = new ArrayList<SelectItem>();

    try
    {
      Tasks tasks = (Tasks) FacesVariableUtil.getValue("tasks");
      
      for (TaskVO task : tasks.getTasksForUserWorkgroups())
      {
        if (!taskNames.contains(task.getTaskName()))
          taskNames.add(task.getTaskName());
      }
      for (String taskName : taskNames)
        taskNameSelects.add(new SelectItem(taskName));
    }
    catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
    
    return taskNameSelects;
  }
  
  public List<SelectItem> getSelectItems()
  {
    return list();
  }

}
