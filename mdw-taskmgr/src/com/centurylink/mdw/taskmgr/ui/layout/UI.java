/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.layout;

/**
 * Base class for user interface layout elements.
 */
public class UI
{
  public UI() 
  {
    
  }
  
  public UI(String name)
  {
    _name = name;
  }
  
  public UI(String id, String name)
  {
    _id = id;
    _name = name;
  }
  
  private String _id;
  public String getId() { return _id; }
  public void setId(String id) { _id = id; }
  
  private String _name;
  public String getName() { return _name; }
  public void setName(String name) { _name = name; }

  private String _model;
  public String getModel() { return _model; }
  public void setModel(String model) { _model = model; }  
}
