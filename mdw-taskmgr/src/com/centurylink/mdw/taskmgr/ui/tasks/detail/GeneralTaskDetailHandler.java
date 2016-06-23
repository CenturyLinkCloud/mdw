/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.detail;

import java.io.IOException;
import java.net.URL;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.taskmgr.ui.tasks.FullTaskInstance;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.util.RemoteLocator;

// TODO generalize and provide hooks for specifying arbitrary XHTML form

public class GeneralTaskDetailHandler
{
  private FullTaskInstance taskInstance;
  public FullTaskInstance getTaskInstance() { return taskInstance; }
  public void setTaskInstance(FullTaskInstance taskInstance) { this.taskInstance = taskInstance; }

  public GeneralTaskDetailHandler(FullTaskInstance taskInstance)
  {
    this.taskInstance = taskInstance;
  }

  /**
   * Returns the task detail form name.
   */
  public String set() throws UIException
  {
    try
    {
      String formDataString = RemoteLocator.getTaskManager().
      		getTaskInstanceData(taskInstance.getTaskInstance()).getContent();
      FormDataDocument formDataDoc = new FormDataDocument();
      formDataDoc.load(formDataString);
      String formName = formDataDoc.getAttribute(FormDataDocument.ATTR_FORM);
      if (formName.endsWith(".xhtml"))
      {
        FacesVariableUtil.setValue("formDataDocument", formDataDoc);
      }

      return formName;
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public String go() throws UIException
  {
    if (set().endsWith(".xhtml"))
    {
      return "go_taskForm";
    }
    else
    {
      String taskManagerUrl = ApplicationContext.getTaskManagerUrl();
      String generalTaskDetailUrl = taskManagerUrl + "/MDWHTTPListener/task?taskInstanceId=" + taskInstance.getInstanceId();
      try
      {
        FacesVariableUtil.navigate(new URL(generalTaskDetailUrl));
      }
      catch (IOException ex)
      {
        throw new UIException(ex.getMessage(), ex);
      }
      return null; // stay on the same page
    }
  }

  public String getMyTaskDetailNavOutcome()
  {
    return "go_myTaskForm";
  }

}
