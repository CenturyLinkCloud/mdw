/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.detail;

public class MapElement implements Comparable<MapElement>
{
  private String _name;
  public String getName() { return _name; }
  public void setName(String name) { _name = name; }
  
  private Object _value;
  public Object getValue() { return _value; }
  public void setValue(Object val) { _value = val; }
  
  public MapElement(String name, Object value)
  {
    _name = name;
    _value = value;
  }
  
  public String toString()
  {
    if (_value == null)
      return null;
    else
      return _value.toString();
  }
  
  public int compareTo(MapElement other)
  {
    return this.getName().compareTo(other.getName());
  }
  
}