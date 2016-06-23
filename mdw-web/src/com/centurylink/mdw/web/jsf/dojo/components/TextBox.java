/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.components;

import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class TextBox extends DojoComponent
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.dojo.TextBox";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.dojo.TextBoxRenderer";

  private String _inputValue;
  private Integer _size;
  private Boolean _required;
  private String _constraints;
  private String _promptMessage;
  private String _invalidMessage;

  public TextBox()
  {
    setRendererType(RENDERER_TYPE);
  }

  public boolean getRendersChildren()
  {
    return false;
  }

  public String getInputValue()
  {
    if (_inputValue != null)
      return _inputValue;
    return FacesVariableUtil.getString(getValueExpression("inputValue"));
  }
  public void setInputValue(String inputValue)
  {
    _inputValue = inputValue;
  }

  public int getSize()
  {
    if (_size != null)
      return _size.intValue();
    return FacesVariableUtil.getInt(getValueExpression("size"));
  }
  public void setSize(int size)
  {
    _size = new Integer(size);
  }

  public boolean isRequired()
  {
    if (_required != null)
      return _required.booleanValue();
    return FacesVariableUtil.getBoolean(getValueExpression("required"));
  }
  public void setRequired(boolean required)
  {
    _required = new Boolean(required);
  }

  public String getConstraints()
  {
    if (_constraints != null)
      return _constraints;
    return FacesVariableUtil.getString(getValueExpression("constraints"));
  }
  public void setConstraints(String constraints)
  {
    _constraints = constraints;
  }

  public String getPromptMessage()
  {
    if (_promptMessage != null)
      return _promptMessage;
    return FacesVariableUtil.getString(getValueExpression("promptMessage"));
  }
  public void setPromptMessage(String promptMessage)
  {
    _promptMessage = promptMessage;
  }

  public String getInvalidMessage()
  {
    if (_invalidMessage != null)
      return _invalidMessage;
    return FacesVariableUtil.getString(getValueExpression("invalidMessage"));
  }
  public void setInvalidMessage(String invalidMessage)
  {
    _invalidMessage = invalidMessage;
  }

  public Object saveState(FacesContext context)
  {
    Object values[] = new Object[7];
    values[0] = super.saveState(context);
    values[1] = _inputValue;
    values[2] = _size;
    values[3] = _required;
    values[4] = _constraints;
    values[5] = _promptMessage;
    values[6] = _invalidMessage;
    return values;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object values[] = (Object[]) state;
    super.restoreState(context, values[0]);
    _inputValue = (String) values[1];
    _size = (Integer) values[2];
    _required = (Boolean) values[3];
    _constraints = (String) values[4];
    _promptMessage = (String) values[5];
    _invalidMessage = (String) values[6];
  }
}
