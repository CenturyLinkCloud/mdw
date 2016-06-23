/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.action;

import java.util.ArrayList;
import java.util.List;

public class TaskStatusAllowableActions
{
  public TaskStatusAllowableActions()
  {
  }

  private String _status;
  public String getStatus() { return _status; }
  public void setStatus(String s) { _status = s; }

  private List<AllowableAction> _allowableActions = new ArrayList<AllowableAction>();
  public List<AllowableAction> getAllowableActions() { return _allowableActions; }

  public void addAllowableAction(AllowableAction taskAction)
  {
    _allowableActions.add(taskAction);
  }
  
  private boolean _autosave;
  public boolean isAutosave() { return _autosave; }
  public void setAutosave(boolean as) { _autosave = as; }
  public void setAutosave(String as) { _autosave = Boolean.parseBoolean(as); }  

  public boolean containsAction(String actionName)
  {
    for (int i = 0; i < _allowableActions.size(); i++)
    {
      if (((AllowableAction)_allowableActions.get(i)).getName().equals(actionName))
        return true;
    }
    return false;
  }
  
  public AllowableAction getAllowableAction(String actionName)
  {
    for (int i = 0; i < _allowableActions.size(); i++)
    {
      AllowableAction allowableAction = (AllowableAction)_allowableActions.get(i);
      if (allowableAction.getName().equals(actionName))
        return allowableAction;
    }
    return null; // not found
  }
  
  public String toString()
  {
    String ret = "status: " + getStatus();
    for (int i = 0; i < getAllowableActions().size(); i++)
    {
      ret += "\n   allowableAction: " + getAllowableActions().get(i); 
    }
    
    return ret;
  }

}
