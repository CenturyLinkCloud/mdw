/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.action;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a possible task for which a task action might be available.
 * Also includes any logical destination options for the action. 
 */
public class ForTask
{
  public ForTask()
  {
  }
  
  private String _taskName;
  public String getTaskName() { return _taskName; }
  public void setTaskName(String s) { _taskName = s; }
  
  private List<TaskDestination> _destinations = new ArrayList<TaskDestination>();
  public List<TaskDestination> getDestinations() { return _destinations; }
  public void setDestinations(List<TaskDestination> l) { _destinations = l; }
  public void addDestination(TaskDestination dest) { _destinations.add(dest); }
  
  public String toString()
  {
    String ret = "  taskName: " + getTaskName() + "  destinations: ";
    for (int i = 0; i < getDestinations().size(); i++)
      ret += getDestinations().get(i) + ", ";
    
    return ret;
  }
  
}
