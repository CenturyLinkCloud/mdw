/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.orders;

import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.tasks.FullTaskInstance;
import com.centurylink.mdw.taskmgr.ui.tasks.list.TaskListScope;
import com.centurylink.mdw.taskmgr.ui.tasks.list.TaskListScopeActionController;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIError;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListActionController;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;

public class OrderTaskListActionController implements ListActionController
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  public String performAction(String action, ListItem listItem) throws UIException
  {
    FullTaskInstance taskInstance = (FullTaskInstance) listItem;
    DetailManager detailManager = DetailManager.getInstance();

    if (action.equals("taskDetail"))
    {
      // update the taskDetail from the list item
      detailManager.setTaskDetail(taskInstance.getInstanceId().toString());

      // determine which task list scope is appropriate
      TaskListScopeActionController scopeController = TaskListScopeActionController.getInstance();
      if (FacesVariableUtil.getCurrentUser().getCuid().equals(taskInstance.getAssignee()))
      {
        scopeController.setTaskListScope(TaskListScope.getUserScope());
        return TaskListScope.getUserScope().getDetailNavigationOutcome();
      }
      else
      {
        scopeController.setTaskListScope(TaskListScope.getWorkgroupScope());
        return TaskListScope.getWorkgroupScope().getDetailNavigationOutcome();
      }
    }
    else
    {
      logger.severe("Unknown order task list link action: " + action);
      UIError error = new UIError("Unknown order task list link action: " + action);
      FacesVariableUtil.setValue("error", error);
      return "go_error";
    }
  }
}
