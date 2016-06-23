/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.layout;

public class FieldUI extends UI
{
  private String _type;
  public String getType() { return _type; }
  public void setType(String s) { _type = s; }

  private String _attribute;
  public String getAttribute() { return _attribute; }
  public void setAttribute(String s) { _attribute = s; }

  private String _modelAttribute;
  public String getModelAttribute() { return _modelAttribute; }
  public void setModelAttribute(String s) { _modelAttribute = s; }

  private String _modelType;
  public String getModelType() { return _modelType; }
  public void setModelType(String s) { _modelType = s; }

  private String _list;
  public String getList() { return _list; }
  public void setList(String s) { _list = s; }

  private String _lister;
  public String getLister() { return _lister; }
  public void setLister(String s) { _lister = s; }

  private String _defaultValue;
  public String getDefaultValue() { return _defaultValue; }
  public void setDefaultValue(String s) { _defaultValue = s; }

  private String _firstItemLabel;
  public String getFirstItemLabel() { return _firstItemLabel; }
  public void setFirstItemLabel(String s) { _firstItemLabel = s; }

  private boolean _expandable;
  public boolean isExpandable() { return _expandable; }
  public void setExpandable(boolean b) { _expandable = b; }
  
  private String _category;
  public String getCategory() { return _category; }
  public void setCategory(String s) { _category = s; }
  
  private int _colspan;
  public int getColspan() { return _colspan; }
  public void setColspan(int i) { _colspan = i; }

  public String toString()
  {
    return "Field:\n"
      + "name: " + getName() + "\n"
      + "attribute: " + getAttribute() + "\n"
      + "modelAttribute: " + getModelAttribute() + "\n"
      + "modelType: " + getModelType() + "\n"
      + "type: " + getType() + "\n"
      + "list: " + getList() + "\n"
      + "lister: " + getLister() + "\n"
      + "defaultValue: " + getDefaultValue() + "\n"
      + "expandable: " + isExpandable() + "\n"
      + "firstItemLabel: " + getFirstItemLabel() + "\n"
      + "category: " + getCategory() + "\n"
      + "colspan: " + getColspan() + "\n";
  }

}
