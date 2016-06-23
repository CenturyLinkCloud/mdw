/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.create;

import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.util.RemoteLocator;

public class TaskCreationActionController implements ActionListener
{
   private static StandardLogger logger = LoggerUtil.getStandardLogger();

   public void processAction(ActionEvent actionEvent) throws AbortProcessingException
   {

    FacesVariableUtil.setValue("taskCreationActionController", this);
    try
    {
      TaskCreate task = (TaskCreate) FacesVariableUtil.getValue("newTask");

      TaskManager taskMgr = RemoteLocator.getTaskManager();
      Long userID = FacesVariableUtil.getCurrentUser().getUserId();

      TaskInstanceVO tivo = taskMgr.createTaskInstance(new Long(task.getTaskId()),
    		  task.getRelatedId(), task.getComments(), task.getDueDate(), userID, null);

      if (tivo != null)
      {
        DetailManager.getInstance().setTaskDetail(tivo.getTaskInstanceId().toString());
        /* reset create a new task page data */
        FacesVariableUtil.setValue("newTask", new TaskCreate());
      }
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }

   }

}
