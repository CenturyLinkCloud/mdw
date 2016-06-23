/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.variable;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.EditableItem;
import com.centurylink.mdw.taskmgr.ui.tasks.TaskItem;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Wraps a model TaskVariable instance to provide the list item functionality
 * for dynamically displaying columns according to the layout configuration.
 */
public class TaskVariableItem extends ListItem implements EditableItem
{
  private VariableVO mVariableVO;
  public VariableVO getVariableVO() { return mVariableVO; }
  public void setVariableVO(VariableVO vVO) { mVariableVO = vVO; }
  
  public TaskVariableItem()
  {
    mVariableVO = new VariableVO(); 
  }
  
  public TaskVariableItem(VariableVO variableVO)
  {
    mVariableVO = variableVO;  
  }
  
  public String getVariableId()
  {
    if (mVariableVO.getVariableId() == null)
      return null;
    
    return mVariableVO.getVariableId().toString();
  }
  
  public void setVariableId(String variableId)
  {
    if (variableId == null || variableId.trim().length() == 0)
    {
      FacesVariableUtil.addMessage("variableSelect", "Please select a variable.");
      return;
    }
    mVariableVO.setVariableId(new Long(variableId));
  }
  
  public String getName()
  {
    return mVariableVO.getVariableName();
  }
  
  public void setName(String variableName)
  {
    mVariableVO.setVariableName(variableName);
  }
  
  public String getReferredAs()
  {
    return mVariableVO.getVariableReferredAs();
  }
  
  public void setReferredAs(String referredAs)
  {
    mVariableVO.setVariableReferredAs(referredAs);
  }

  public Boolean getEditability()
  {
    return new Boolean(isEditable());
  }
  
  public boolean isEditable()
  {
    return VariableVO.DATA_REQUIRED.equals(mVariableVO.getDisplayMode())
    	|| VariableVO.DATA_OPTIONAL.equals(mVariableVO.getDisplayMode());
  }
  
  public String getEditableDisplay()
  {
    if (isEditable())
      return "Yes";
    else
      return "No";
  }
  
  public void setEditability(Boolean editability)
  {
    if (editability.booleanValue()) {
    	if (mVariableVO.getDisplayMode().equals(VariableVO.DATA_READONLY))
    		mVariableVO.setDisplayMode(VariableVO.DATA_REQUIRED);
    } else mVariableVO.setDisplayMode(VariableVO.DATA_READONLY);
  }
  
  public Boolean getOptionality()
  {
    return new Boolean(isOptional());
  }
  
  public boolean isOptional()
  {
    return VariableVO.DATA_OPTIONAL.equals(mVariableVO.getDisplayMode());
  }
  
  public String getOptionalDisplay()
  {
    if (isOptional())
      return "Yes";
    else
      return "No";
  }
  
  public void setOptionality(Boolean optionality)
  {
    Integer opt = optionality.booleanValue() ? VariableVO.DATA_OPTIONAL : VariableVO.DATA_REQUIRED;
    mVariableVO.setDisplayMode(opt);
  }
  
  public Integer getDisplaySequence()
  {
    return mVariableVO.getDisplaySequence();
  }
  
  public void setDisplaySequence(Integer displaySequence)
  {
    mVariableVO.setDisplaySequence(displaySequence);
  }
  
  public void add() throws UIException
  {
    if (getVariableId() == null || getVariableId().equals("0"))
    {
      FacesVariableUtil.addMessage("variableSelect", "Please select a variable.");
      return;
    }
      
    Long variableId = new Long(getVariableId());
    TaskItem item = (TaskItem) FacesVariableUtil.getValue("taskItem");
    
    if (item.hasVariable(variableId))
    {
      FacesVariableUtil.addMessage("variableSelect", "Variable " + variableId + " is already mapped to task.");
      return;
    }
    
    try
    {
      Integer displayMode = isEditable() ? 
      		  (isOptional() ? VariableVO.DATA_OPTIONAL : VariableVO.DATA_REQUIRED) : 
      			  VariableVO.DATA_READONLY;
      TaskManager varMgr = RemoteLocator.getTaskManager();
      varMgr.createVariableMapping(item.getId(), OwnerType.TASK, variableId, getReferredAs(), displayMode, getDisplaySequence());
      item.setVariables(varMgr.getVariablesForTask(item.getId()));
      auditLogUserAction(Action.Change, Entity.Variable, getId(), getName() + " (Task Mapping)");      
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
      TaskItem item = (TaskItem) FacesVariableUtil.getValue("taskItem");
      TaskManager varMgr = RemoteLocator.getTaskManager();
      varMgr.deleteVariableMapping(item.getId(), OwnerType.TASK, new Long(getVariableId()));
      item.setVariables(varMgr.getVariablesForTask(item.getId()));
      auditLogUserAction(Action.Delete, Entity.Variable, getId(), getName() + " (Task Mapping)");      
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
      Integer displayMode = isEditable() ? 
    		  (isOptional() ? VariableVO.DATA_OPTIONAL : VariableVO.DATA_REQUIRED) : 
    			  VariableVO.DATA_READONLY;
      TaskManager varMgr = RemoteLocator.getTaskManager();
      TaskItem item = (TaskItem) FacesVariableUtil.getValue("taskItem");
      varMgr.updateVariableMapping(item.getId(), OwnerType.TASK, new Long(getVariableId()), 
          getReferredAs(), displayMode, getDisplaySequence());
      item.setVariables(varMgr.getVariablesForTask(item.getId()));
      auditLogUserAction(Action.Create, Entity.Variable, getId(), getName() + " (Task Mapping)");      
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public boolean isEditableByCurrentUser()
  {
    return false;
  }
}
