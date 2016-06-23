/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.components;

import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class MenuItem extends DojoComponent
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.dojo.MenuItem";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.dojo.MenuItemRenderer";

  private String _checkedState;  // not boolean because null is allowed

  public MenuItem()
  {
    setRendererType(RENDERER_TYPE);
  }

  public String getCheckedState()
  {
    if (_checkedState != null)
      return _checkedState;
    return FacesVariableUtil.getString(getValueExpression("checkedState"));
  }
  public void setCheckedState(String checkedState)
  {
    _checkedState = checkedState;
  }

  public Object saveState(FacesContext context)
  {
    Object values[] = new Object[2];
    values[0] = super.saveState(context);
    values[1] = _checkedState;
    return values;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object values[] = (Object[]) state;
    super.restoreState(context, values[0]);
    _checkedState = (String) values[1];
  }
}
