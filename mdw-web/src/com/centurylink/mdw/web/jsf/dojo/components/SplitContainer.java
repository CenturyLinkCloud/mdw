/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.components;

import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class SplitContainer extends DojoLayout
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.dojo.SplitContainer";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.dojo.SplitContainerRenderer";

  private String _orientation;
  private Integer _sizerWidth;
  private Boolean _activeSizing;

  public String getOrientation()
  {
    if (_orientation != null)
      return _orientation;
    return FacesVariableUtil.getString(getValueExpression("orientation"), "horizontal");
  }
  public void setOrientation(String orientation)
  {
    _orientation = orientation;
  }

  public int getSizerWidth()
  {
    if (_sizerWidth != null)
      return _sizerWidth.intValue();
    return FacesVariableUtil.getInt(getValueExpression("sizerWidth"), 7);
  }
  public void setSizerWidth(int sizerWidth)
  {
    _sizerWidth = new Integer(sizerWidth);
  }

  public boolean isActiveSizing()
  {
    if (_activeSizing != null)
      return _activeSizing.booleanValue();
    return FacesVariableUtil.getBoolean(getValueExpression("activeSizing"));
  }
  public void setActiveSizing(boolean activeSizing)
  {
    _activeSizing = new Boolean(activeSizing);
  }

  public Object saveState(FacesContext context)
  {
    Object values[] = new Object[4];
    values[0] = super.saveState(context);
    values[1] = _orientation;
    values[2] = _sizerWidth;
    values[3] = _activeSizing;
    return values;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object values[] = (Object[]) state;
    super.restoreState(context, values[0]);
    _orientation = (String) values[1];
    _sizerWidth = (Integer) values[2];
    _activeSizing = (Boolean) values[3];
  }
}
