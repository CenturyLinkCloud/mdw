/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.variable;

import com.centurylink.mdw.taskmgr.ui.EditableItemActionController;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

public class TaskVariableActionController extends EditableItemActionController
{
  /**
   * Called from command links in the users list.
   * @return the nav destination.
   */
  public String performAction(String action, ListItem listItem) throws UIException
  {
    super.performAction(action, listItem);
    FacesVariableUtil.setValue("taskVariableActionController", this);
    FacesVariableUtil.setValue("taskVariableItem", getItem());
    return performAction();
  }

  /**
   * Called from the command buttons for editing.
   * @return the nav destination
   */
  public String performAction() throws UIException
  {
    setItem((TaskVariableItem)FacesVariableUtil.getValue("taskVariableItem"));

    // all actions are covered in the base type
    super.performAction();

    if (getAction().equals(ACTION_LIST))
    {
      FacesVariableUtil.setValue("taskVariableItem", new TaskVariableItem());
    }
    else if (getAction().equals(ACTION_ERROR))
    {
      return "go_error";
    }

    return "go_tasks";
  }

  public String add() throws UIException
  {
    preserveTaskRowParam();
    return super.add();
  }

  public String cancel() throws UIException
  {
    preserveTaskRowParam();
    return super.cancel();
  }

  public String confirmDelete() throws UIException
  {
    preserveTaskRowParam();
    return super.confirmDelete();
  }

  public String save() throws UIException
  {
    preserveTaskRowParam();
    return super.save();
  }
  
  private void preserveTaskRowParam()
  {
    String taskCurRow = (String) FacesVariableUtil.getValue("dataTableCurrentRow_taskList");
    FacesVariableUtil.setRequestAttrValue("dataTableCurrentRow_taskList", taskCurRow);
  }
  
}
