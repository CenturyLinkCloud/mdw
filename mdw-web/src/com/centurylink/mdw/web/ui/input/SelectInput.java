/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui.input;

import java.util.List;

import javax.faces.model.SelectItem;

public class SelectInput extends Input
{
  private List<SelectItem> _selectValues;
  public List<SelectItem> getSelectValues() { return _selectValues; }
  public void setSelectValues(List<SelectItem> l) { _selectValues = l; }
  
  public String getSelectedLabel()
  {
    for (int i = 0; i < _selectValues.size(); i++)
    {
      SelectItem item = _selectValues.get(i);
      if (item.getValue().equals(getValue()))
        return item.getLabel();
    }
    return null;
  }

  public SelectInput(String attribute, String label, List<SelectItem> values)
  {
    super(attribute, label);
    _selectValues = values;
  }

}
