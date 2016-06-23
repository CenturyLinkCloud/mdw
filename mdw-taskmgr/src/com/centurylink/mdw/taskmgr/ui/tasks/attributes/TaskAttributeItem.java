/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.attributes;

import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.EditableItem;
import com.centurylink.mdw.taskmgr.ui.tasks.TaskItem;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Wraps a model Attribute instance to provide the list item functionality for 
 * dynamically displaying columns according to the layout configuration.
 */
public class TaskAttributeItem extends ListItem implements EditableItem
{
  public TaskAttributeItem()
  {
  }
  
  public TaskAttributeItem(AttributeVO attrVO)
  {
    this.setName(attrVO.getAttributeName());
    this.setComment(attrVO.getAttributeValue());
    this.setId(attrVO.getAttributeId());
  }
  
  private Integer _typeId = new Integer(2);
  public String getTypeId()
  {
    return _typeId == null ? null : _typeId.toString();
  }
  public void setTypeId(String typeId)
  {
    _typeId = new Integer(typeId);
  }  
  public String getType()
  {
      return "Workflow";
  }
  
  /**
   * Method that adds an attribute
   */
  public void add() throws UIException
  {
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      TaskItem item = (TaskItem) FacesVariableUtil.getValue("taskItem");
      taskMgr.addTaskAttribute(item.getId(), getName(), getComment(), _typeId);
      auditLogUserAction(Action.Create, Entity.Attribute, new Long(0), getName() + " (Task Mapping)");      
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  /**
   * Method that deletes the task attrib
   */
  public void delete() throws UIException
  {
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      taskMgr.deleteTaskAttribute(this.getId());
      auditLogUserAction(Action.Delete, Entity.Attribute, getId(), getName() + " (Task Mapping)");      
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  /**
   * Method that updates the attrib
   */
  public void save() throws UIException
  {
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      taskMgr.updateTaskAttribute(getId(), getName(), getComment(), _typeId);
      auditLogUserAction(Action.Change, Entity.Attribute, getId(), getName() + " (Task Mapping)");      
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
