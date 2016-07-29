/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui.input;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.utilities.StringHelper;

public class MultiSelectInput extends Input
{
  private List<SelectItem> _selectValues;
  public List<SelectItem> getSelectValues() { return _selectValues; }
  public void setSelectValues(List<SelectItem> l) { _selectValues = l; }

  public List<String> getSelectedLabel()
  {
    List<String> selectedValues = null;
    for (int i = 0; i < _selectValues.size(); i++)
    {
      SelectItem item = _selectValues.get(i);
      if (item.getValue().equals(getValue())) {
        if (selectedValues == null)
          selectedValues = new ArrayList<String>();
        selectedValues.add(item.getLabel());
      }
    }
    return selectedValues;
  }

  public MultiSelectInput(String attribute, String label, List<SelectItem> values)
  {
    super(attribute, label);
    _selectValues = values;
  }

  @Override
  public void setValue(Object o) {

    if (o instanceof String) {
      String[] strArr = ((String)o).split(":");
      super.setValue(strArr);
    }
    else
      super.setValue(o);
  }

  private String parseArrayList(boolean forQuery) {
    if (isValueEmpty())
      return "";

    String value = "";

    for (String item : (String[])getValue()) {
      if (value.length() > 0)
        value += ":";

      if (forQuery)
        value += "'" + item + "'";
      else
        value += item;
    }
    return value;
  }

  public String getSelectedStringList() {
    return parseArrayList(true);
  }

  @Override
  public String toString() {
    return parseArrayList(false);
  }

  public int getSelectValuesSize() {
    return getSelectValues().size();
  }
}
