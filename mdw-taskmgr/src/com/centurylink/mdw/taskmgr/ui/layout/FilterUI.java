/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.layout;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.web.ui.UIException;

public class FilterUI extends UI
{
  public FilterUI() { }

  private List<FieldUI> _fields = new ArrayList<FieldUI>();
  public List<FieldUI> getFields() { return _fields; }
  
  private String _width;
  public String getWidth() { return _width; }

  public void addField(FieldUI field)
  {
    _fields.add(field);
  }

  public void addFilterUI(String id, String name, String model, String width)
  throws UIException
  {
    setId(id);
    setName(name);
    setModel(model);
    _width = width;
    ViewUI.getInstance().addFilterUI(id, this);
  }

}
