/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.user;

import java.util.Map;

/**
 * Skeletal report object for specifying report name and version in a managed bean.
 */
public class UserReport
{
  private String name;
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  
  private int version;
  public int getVersion() { return version; }
  public void setVersion(int version) { this.version = version; }
  
  private Map<String,String> params;
  public Map<String,String> getParams() { return params; }
  public void setParams(Map<String,String> params) { this.params = params; }
}