/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.attributes;

import com.centurylink.mdw.taskmgr.ui.EditableItemActionController;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

public class TaskAttributeActionController extends EditableItemActionController
{
  /**
   * Called from command links in the attributes list.
   * 
   * @return the nav destination.
   */
  public String performAction(String action, ListItem listItem) throws UIException
  {
    super.performAction(action, listItem);
    FacesVariableUtil.setValue("taskAttributeActionController", this);
    FacesVariableUtil.setValue("taskAttributeItem", getItem());

    return performAction();
  }

  /**
   * Called from the command buttons for editing.
   * 
   * @return the nav destination
   */
  public String performAction() throws UIException
  {
    setItem((TaskAttributeItem)FacesVariableUtil.getValue("taskAttributeItem"));

    // all actions are covered in the base type
    super.performAction();

    if (getAction().equals(ACTION_LIST))
    {
      FacesVariableUtil.setValue("taskAttributeItem", new TaskAttributeItem());
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
