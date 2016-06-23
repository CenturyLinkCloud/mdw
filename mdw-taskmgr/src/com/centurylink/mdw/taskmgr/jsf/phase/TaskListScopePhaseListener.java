/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.jsf.phase;

import javax.faces.context.ExternalContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import com.centurylink.mdw.taskmgr.ui.tasks.list.TaskListScope;
import com.centurylink.mdw.taskmgr.ui.tasks.list.TaskListScopeActionController;

/**
 * Phase listener to update the task list scope as appropriate
 * depending on the view id of the requested facelet.
 */
public class TaskListScopePhaseListener implements PhaseListener
{
  private static final long serialVersionUID = 5338921312193423182L;

  public PhaseId getPhaseId()
  {
    return PhaseId.RESTORE_VIEW;
  }

  public void beforePhase(PhaseEvent event)
  {
    // get a handle to the action controller
    TaskListScopeActionController controller = TaskListScopeActionController.getInstance();
    ExternalContext externalContext = event.getFacesContext().getExternalContext();

    // check if tab explicitly demanded via a request param
    String requestTab = (String) externalContext.getRequestParameterMap().get("tmTabbedPage");
    if (requestTab != null)
    {
      if (requestTab.equals("myTasksTab"))
      {
        controller.setTaskListScope(TaskListScope.getUserScope());
        return;
      }
      else if (requestTab.equals("workgroupTasksTab"))
      {
        controller.setTaskListScope(TaskListScope.getWorkgroupScope());
        return;
      }
    }

    // otherwise check for requested view matching myTasks or workgroupTasks
    String path = externalContext.getRequestServletPath();
    if (path != null)
    {
      if (path.endsWith("myTasks.jsf"))
      {
        controller.setTaskListScope(TaskListScope.getUserScope());
        return;
      }
      else if (path.endsWith("workgroupTasks.jsf"))
      {
        controller.setTaskListScope(TaskListScope.getWorkgroupScope());
        return;
      }
    }
  }

  public void afterPhase(PhaseEvent event)
  {
  }
}