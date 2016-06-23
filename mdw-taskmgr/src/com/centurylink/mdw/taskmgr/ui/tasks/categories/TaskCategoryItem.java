/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.categories;

import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.EditableItem;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Wraps a model TaskCategory instance to provide the list item functionality 
 * for dynamically displaying columns according to the layout configuration.
 */
public class TaskCategoryItem extends ListItem implements EditableItem
{
  public TaskCategoryItem()
  {
    super();
  }

  public TaskCategoryItem(TaskCategory pTaskCat)
  {
    super();
    super.setId(pTaskCat.getId());
    super.setName(pTaskCat.getCode());
    super.setComment(pTaskCat.getDescription());
  }

  public void add() throws UIException
  {
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      taskMgr.createTaskCategory(this.getName(), this.getComment());
      auditLogUserAction(Action.Create, Entity.Category, getId(), getName());      
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public void delete() throws UIException
  {
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      taskMgr.deleteTaskCategory(this.getId());
      auditLogUserAction(Action.Delete, Entity.Category, getId(), getName());      
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public void save() throws UIException
  {
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      taskMgr.updateTaskCategory(this.getId(), this.getName(), this.getComment());
      auditLogUserAction(Action.Change, Entity.Category, getId(), getName());      
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public boolean isEditableByCurrentUser()
  {
    AuthenticatedUser authUser = FacesVariableUtil.getCurrentUser();
    return authUser.isInRoleForAnyGroup(UserRoleVO.PROCESS_DESIGN);
  }
  
}
