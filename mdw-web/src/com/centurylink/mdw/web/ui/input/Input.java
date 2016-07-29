/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui.input;

public abstract class Input
{
  private int _sequenceId = -1;
  public int getSequenceId() { return _sequenceId; }
  public void setSequenceId(int i) { this._sequenceId = i; }

  private String _attribute;
  public String getAttribute() { return _attribute; }
  public void setAttribute(String s) { _attribute = s; }

  private String _modelAttribute;
  public String getModelAttribute() { return _modelAttribute; }
  public void setModelAttribute(String s) { _modelAttribute = s; }

  private String _modelType;
  public String getModelType() { return _modelType; }
  public void setModelType(String s) { _modelType = s; }

  private Object _value;
  public Object getValue() { return _value; }
  public void setValue(Object o) { _value = o; }

  private String _label;
  public String getLabel() { return _label; }
  public void setLabel(String s) { _label = s; }

  private int _width;
  public int getWidth() { return _width; }
  public void setWidth(int i) { _width = i; }

  private boolean _expandable;
  public boolean isExpandable() { return _expandable; }
  public void setExpandable(boolean b) { _expandable = b; }

  private boolean _hidden;
  public boolean isHidden() { return _hidden; }
  public void setHidden(boolean b) { _hidden = b; }

  private String _category;
  public String getCategory() { return _category; }
  public void setCategory(String s) { _category = s; }

  private int _colspan;
  public int getColspan() { return _colspan; }
  public void setColspan(int i) { _colspan = i; }

  public Input(String attribute, String label)
  {
    this._attribute = attribute;
    this._label = label;
  }

  public boolean isInputTypeText()
  {
    return this instanceof TextInput;
  }

  public boolean isInputTypeSelect()
  {
    return this instanceof SelectInput;
  }

  public boolean isInputTypeMultiSelect()
  {
    return this instanceof MultiSelectInput;
  }

  public boolean isInputTypeDate()
  {
    return this instanceof DateInput;
  }

  public boolean isInputTypeDateRange()
  {
    return this instanceof DateRangeInput;
  }

  public boolean isInputTypeDigit()
  {
    return this instanceof DigitInput;
  }

  public boolean isInputTypeTn()
  {
    return this instanceof TnInput;
  }

  public boolean isValueEmpty()
  {
    return getValue() == null
      || getValue().toString().trim().length() == 0
      || (isInputTypeSelect() && getValue().equals("0"))
      || (isInputTypeMultiSelect() && ((String[])getValue()).length == 1 && ((String[])getValue())[0].equals("0"));
  }
}
