/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.ajax.components;

import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.jsf.dojo.components.DojoComponent;

public class ProjectPanel extends DojoComponent
{
  public static final String COMPONENT_FAMILY = "com.centurylink.mdw.Ajax";
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.ajax.ProjectPanel";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.ajax.ProjectPanelRenderer";

  private String _dataUrl;

  public ProjectPanel()
  {
    setRendererType(RENDERER_TYPE);
  }

  public String getFamily()
  {
    return COMPONENT_FAMILY;
  }

  public boolean getRendersChildren()
  {
    return false;
  }

  public String getDataUrl()
  {
    if (_dataUrl != null)
      return _dataUrl;
    return FacesVariableUtil.getString(getValueExpression("exportLink"));
  }
  public void setDataUrl(String dataUrl)
  {
    _dataUrl = dataUrl;
  }

  public Object saveState(FacesContext context)
  {
    Object values[] = new Object[2];
    values[0] = super.saveState(context);
    values[1] = _dataUrl;
    return values;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object values[] = (Object[]) state;
    super.restoreState(context, values[0]);
    _dataUrl = (String) values[1];
  }

}
