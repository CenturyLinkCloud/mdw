/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.action;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents an allowable task action as defined in TaskActions.xml.
 */
public class AllowableAction
{
  private String _name;
  public String getName() { return _name; }
  public void setName(String name) { _name = name; }
  
  private String _navigationOutcome;
  public String getNavigationOutcome() { return _navigationOutcome; }
  public void setNavigationOutcome(String no) { _navigationOutcome = no; }
  
  private boolean _autosave;
  public boolean isAutosave() { return _autosave; }
  public void setAutosave(boolean as) { _autosave = as; }
  public void setAutosave(String as) { _autosave = Boolean.parseBoolean(as); }
  
  private String _alias;
  public String getAlias() { return _alias; }
  public void setAlias(String a) { _alias = a; }
  
  private String _outcome;
  public String getOutcome() { return _outcome; }
  public void setOutcome(String o) { _outcome = o; }
  
  private boolean _requireComment;
  public boolean isRequireComment() { return _requireComment; }
  public void setRequireComment(boolean rq) { _requireComment = rq; }
  
  private List<ForTask> _forTasks = new ArrayList<ForTask>();
  public List<ForTask> getForTasks() { return _forTasks; }
  public void setForTasks(List<ForTask> l) { _forTasks = l; }
  public void addForTask(ForTask forTask) { _forTasks.add(forTask); } 
      
  public String toString()
  {
    String ret = "name: " + getName()
      + "  alias: " + getAlias()
      + "  requireComment: " + isRequireComment()
      + "  outcome: " + getOutcome()
      + "  navigationOutcome: " + getNavigationOutcome()
      + "  autosave: " + isAutosave()
      + "\nforTasks: \n";
    for (int i = 0; i < getForTasks().size(); i++)
      ret += getForTasks().get(i);
    
    return ret;
  }
  
}
