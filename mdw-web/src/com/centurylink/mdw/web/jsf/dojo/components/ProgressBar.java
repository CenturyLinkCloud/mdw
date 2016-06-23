/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.components;

import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class ProgressBar extends DojoComponent
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.dojo.ProgressBar";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.dojo.ProgressBarRenderer";

  private String _jsId;
  private Boolean _indeterminate;

  public ProgressBar()
  {
    setRendererType(RENDERER_TYPE);
  }

  public String getJsId()
  {
    if (_jsId != null)
      return _jsId;
    return FacesVariableUtil.getString(getValueExpression("jsId"));
  }
  public void setJsId(String jsId)
  {
    _jsId = jsId;
  }

  public boolean isIndeterminate()
  {
    if (_indeterminate != null)
      return _indeterminate.booleanValue();
    return FacesVariableUtil.getBoolean(getValueExpression("indeterminate"));
  }
  public void setIndeterminate(boolean indeterminate)
  {
    _indeterminate = new Boolean(indeterminate);
  }

  public Object saveState(FacesContext context)
  {
    Object values[] = new Object[3];
    values[0] = super.saveState(context);
    values[1] = _jsId;
    values[2] = _indeterminate;
    return values;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object values[] = (Object[]) state;
    super.restoreState(context, values[0]);
    _jsId = (String) values[1];
    _indeterminate = (Boolean) values[2];
  }
}