/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.components;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public abstract class DojoComponent extends UIComponentBase
{
  public static final String COMPONENT_FAMILY = "com.centurylink.mdw.Dojo";

  private String _id;
  private String _label;
  private String _labelClass;
  private String _align;
  private String _styleClass;
  private String _style;
  private String _dojoType;
  private String _onclick;
  private String _onchange;
  private String _onmouseover;
  private Boolean _disabled;

  public String getFamily()
  {
    return COMPONENT_FAMILY;
  }

  public String getId()
  {
    if (_id != null)
      return _id;
    return FacesVariableUtil.getString(getValueExpression("id"));
  }
  public void setId(String id)
  {
    _id = id;
  }

  public String getLabel()
  {
    if (_label != null)
      return _label;
    return FacesVariableUtil.getString(getValueExpression("label"));
  }
  public void setLabel(String label)
  {
    _label = label;
  }

  public String getLabelClass()
  {
    if (_labelClass != null)
      return _labelClass;
    return FacesVariableUtil.getString(getValueExpression("labelClass"));
  }
  public void setLabelClass(String labelClass)
  {
    _labelClass = labelClass;
  }

  public String getAlign()
  {
    if (_align != null)
      return _align;
    return FacesVariableUtil.getString(getValueExpression("align"));
  }
  public void setAlign(String align)
  {
    _align = align;
  }

  public String getStyleClass()
  {
    if (_styleClass != null)
      return _styleClass;
    return FacesVariableUtil.getString(getValueExpression("styleClass"));
  }
  public void setStyleClass(String styleClass)
  {
    _styleClass = styleClass;
  }

  public String getStyle()
  {
    if (_style != null)
      return _style;
    return FacesVariableUtil.getString(getValueExpression("style"));
  }
  public void setStyle(String style)
  {
    _style = style;
  }

  public String getDojoType()
  {
    if (_dojoType != null)
      return _dojoType;
    return FacesVariableUtil.getString(getValueExpression("dojoType"));
  }
  public void setDojoType(String dojoType)
  {
    _dojoType = dojoType;
  }

  public String getOnclick()
  {
    if (_onclick != null)
      return _onclick;
    return FacesVariableUtil.getString(getValueExpression("onclick"));
  }
  public void setOnclick(String onclick)
  {
    _onclick = onclick;
  }

  public String getOnchange()
  {
    if (_onchange != null)
      return _onchange;
    return FacesVariableUtil.getString(getValueExpression("onchange"));
  }
  public void setOnchange(String onchange)
  {
    _onchange = onchange;
  }

  public String getOnmouseover()
  {
    if (_onmouseover != null)
      return _onmouseover;
    return FacesVariableUtil.getString(getValueExpression("onmouseover"));
  }
  public void setOnmouseover(String onmouseover)
  {
    _onmouseover = onmouseover;
  }

  public boolean isDisabled()
  {
    if (_disabled != null)
      return _disabled.booleanValue();
    return FacesVariableUtil.getBoolean(getValueExpression("disabled"));
  }
  public void setDisabled(boolean disabled)
  {
    _disabled = new Boolean(disabled);
  }

  public boolean getRendersChildren()
  {
    return true;
  }

  /**
   * Invoked after the render phase has completed, this method returns an
   * object which can be passed to the restoreState of some other instance of
   * UIComponentBase to reset that object's state to the same values as this
   * object currently has.
   */
  public Object saveState(FacesContext context)
  {
    Object values[] = new Object[12];
    values[0] = super.saveState(context);
    values[1] = _id;
    values[2] = _label;
    values[3] = _labelClass;
    values[4] = _align;
    values[5] = _styleClass;
    values[6] = _style;
    values[7] = _dojoType;
    values[8] = _onchange;
    values[9] = _onchange;
    values[10] = _onmouseover;
    values[11] = _disabled;
    return values;
  }

  /**
   * Invoked in the restore view phase, this method initialises this object's
   * members from the values saved previously into the provided state object.
   *
   * @param state an object previously returned by the saveState method of this class
   */
  public void restoreState(FacesContext context, Object state)
  {
    Object values[] = (Object[]) state;
    super.restoreState(context, values[0]);
    _id = (String) values[1];
    _label = (String) values[2];
    _labelClass = (String) values[3];
    _align = (String) values[4];
    _styleClass = (String) values[5];
    _style = (String) values[6];
    _dojoType = (String) values[7];
    _onclick = (String) values[8];
    _onchange = (String) values[9];
    _onmouseover = (String) values[10];
    _disabled = (Boolean) values[11];
  }

}