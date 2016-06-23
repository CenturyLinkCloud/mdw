/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.components;

import javax.faces.component.UIColumn;
import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;

/**
 * Component representing an individual filter criterion.
 */
public class Criteria extends UIColumn
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.web.jsf.components.Criteria";

  private String _label;
  public void setLabel(String s) { _label = s; }
  public String getLabel()
  {
    if (_label != null)
      return _label;
    return FacesVariableUtil.getString(getValueExpression("label"));
  }

  private String _name;
  public void setName(String s) { _name = s; }
  public String getName()
  {
    if (_name != null)
      return _name;
    return FacesVariableUtil.getString(getValueExpression("name"));
  }

  public Object saveState(FacesContext context)
  {
    Object[] values = new Object[3];
    values[0] = super.saveState(context);
    values[1] = _label;
    values[2] = _name;
    return values;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object[] values = (Object[]) state;
    super.restoreState(context, values[0]);
    _label = (String) values[1];
    _name = (String) values[2];
  }

}
