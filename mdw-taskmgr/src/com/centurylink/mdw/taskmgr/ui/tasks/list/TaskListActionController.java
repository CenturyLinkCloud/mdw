/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.list;

import java.io.IOException;
import java.net.URL;

import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.process.ProcessDesignerView;
import com.centurylink.mdw.taskmgr.ui.tasks.FullTaskInstance;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.CustomPageDetailHandler;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.GeneralTaskDetailHandler;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIError;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListActionController;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;

/**
 * Action handler for link click events on the task lists.
 */
public class TaskListActionController implements ListActionController
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
      taskInstance = detailManager.getTaskDetail().getFullTaskInstance();

      if (taskInstance.isSummaryOnly())
      {
        try
        {
          TaskListScope scope = TaskListScopeActionController.getInstance().getTaskListScope();
          FacesVariableUtil.navigate(new URL(taskInstance.getRemoteDetailUrl() + "&tmTabbedPage=" + scope.getTabbedPage()));
        }
        catch (IOException ex)
        {
          throw new UIException(ex.getMessage(), ex);
        }
        return null;
      }
      else if (taskInstance.isAutoformTask())
      {
        return "go_taskDetail";
      }
      else if (taskInstance.isGeneralTask())
      {
        String navOutcome = new GeneralTaskDetailHandler(taskInstance).go();
        detailManager.getTaskDetail().setNavOutcome(navOutcome == null ? "" : navOutcome);
        return navOutcome;
      }
      else if (TaskTemplateCache.getTaskTemplate(taskInstance.getTaskId()).isHasCustomPage())
      {
        String navOutcome = getCustomPageDetailHandler(taskInstance).go();
        detailManager.getTaskDetail().setNavOutcome(navOutcome);
        return navOutcome;
      }
      else
      {
        return "go_taskDetail";
      }
    }
    else if (action.equals("orderDetail"))
    {
      // update the orderDetail from the list item
      detailManager.setOrderDetail(taskInstance.getOrderId());
      return "go_tasksForOrder";
    }
    else if (action.equals("processInstance"))
    {
      // open designer
      ProcessDesignerView designerView = (ProcessDesignerView) FacesVariableUtil.getValue("processDesignerView");
      designerView.launchDesigner(taskInstance);
      return null;
    }
    else
    {
      logger.severe("Unknown task list link action: " + action);
      UIError error = new UIError("Unknown task list link action: " + action);
      FacesVariableUtil.setValue("error", error);
      return "go_error";
    }
  }

  protected CustomPageDetailHandler getCustomPageDetailHandler(FullTaskInstance taskInstance)
  {
    return new CustomPageDetailHandler(taskInstance);
  }

}
