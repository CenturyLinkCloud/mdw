/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.categories;

import com.centurylink.mdw.taskmgr.ui.EditableItemActionController;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

public class TaskCategoryActionController
    extends EditableItemActionController
{
  /**
   * Called from command links in the categories list.
   * 
   * @return the nav destination.
   */
  public String performAction(String action, ListItem listItem) throws UIException
  {
    super.performAction(action, listItem);
    FacesVariableUtil.setValue("taskCategoryActionController", this);
    FacesVariableUtil.setValue("taskCategoryItem", getItem());
    return performAction();
  }

  /**
   * Called from the command buttons for editing.
   * 
   * @return the nav destination
   */
  public String performAction() throws UIException
  {
    setItem((TaskCategoryItem) FacesVariableUtil.getValue("taskCategoryItem"));

    // all actions are covered in the base type
    super.performAction();
    if (getAction().equals(ACTION_LIST))
    {
      FacesVariableUtil.setValue("taskCategoryItem", new TaskCategoryItem());
    }
    else if (getAction().equals(ACTION_ERROR))
    {
      return "go_error";
    }

    return "go_taskCategory";
  }

}
