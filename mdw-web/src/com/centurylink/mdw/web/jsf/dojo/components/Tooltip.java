/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.components;

import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class Tooltip extends DojoComponent
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.dojo.Tooltip";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.dojo.TooltipRenderer";

  private String _connectId;

  public Tooltip()
  {
    setRendererType(RENDERER_TYPE);
  }

  public String getConnectId()
  {
    if (_connectId != null)
      return _connectId;
    return FacesVariableUtil.getString(getValueExpression("connectId"));
  }
  public void setConnectId(String connectId)
  {
    _connectId = connectId;
  }


  public Object saveState(FacesContext context)
  {
    Object values[] = new Object[2];
    values[0] = super.saveState(context);
    values[1] = _connectId;
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
    _connectId = (String) values[1];
  }

}