/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.components;

import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class DojoLayout extends DojoComponent
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.dojo.DojoLayout";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.dojo.DojoLayoutRenderer";

  private String _layoutAlign;
  private Integer _sizeShare;

  public DojoLayout()
  {
    setRendererType(RENDERER_TYPE);
  }

  public String getLayoutAlign()
  {
    if (_layoutAlign != null)
      return _layoutAlign;
    return FacesVariableUtil.getString(getValueExpression("layoutAlign"));
  }
  public void setLayoutAlign(String layoutAlign)
  {
    _layoutAlign = layoutAlign;
  }

  public int getSizeShare()
  {
    if (_sizeShare != null)
      return _sizeShare.intValue();
    return FacesVariableUtil.getInt(getValueExpression("sizeShare"));
  }
  public void setSizeShare(int sizeShare)
  {
    _sizeShare = new Integer(sizeShare);
  }

  public Object saveState(FacesContext context)
  {
    Object values[] = new Object[3];
    values[0] = super.saveState(context);
    values[1] = _layoutAlign;
    values[2] = _sizeShare;
    return values;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object values[] = (Object[]) state;
    super.restoreState(context, values[0]);
    _layoutAlign = (String) values[1];
    _sizeShare = (Integer) values[2];
  }

}
