/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.components;

import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class Button extends DojoComponent
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.dojo.Button";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.dojo.ButtonRenderer";

  private String _iconClass;
  private String _title;

  public Button()
  {
    setRendererType(RENDERER_TYPE);
  }

  public String getIconClass()
  {
    if (_iconClass != null)
      return _iconClass;
    return FacesVariableUtil.getString(getValueExpression("iconClass"));
  }
  public void setIconClass(String iconClass)
  {
    _iconClass = iconClass;
  }

  public String getTitle()
  {
    if (_title != null)
      return _title;
    return FacesVariableUtil.getString(getValueExpression("title"));
  }
  public void setTitle(String title)
  {
    _title = title;
  }

  public Object saveState(FacesContext context)
  {
    Object values[] = new Object[3];
    values[0] = super.saveState(context);
    values[1] = _iconClass;
    values[2] = _title;
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
    _iconClass = (String) values[1];
    _title = (String) values[2];
  }

}