/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.action;

public class TaskDestination
{
  private String name;
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  
  public String alias;
  public String getAlias() { return alias; }
  public void setAlias(String alias) { this.alias = alias; }
  
  public String getLabel()
  {
    return alias == null ? name : alias;
  }

  public String toString()
  {
    return "name: " + name + "  alias: " + alias;
  }
}
