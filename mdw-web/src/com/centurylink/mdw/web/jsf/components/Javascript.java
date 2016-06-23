/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.components;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class Javascript extends UIOutput
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.web.jsf.components.Javascript";

  private String _fileLocation;
  public void setFileLocation(String s) { _fileLocation = s; }
  public String getFileLocation()
  {
    if (_fileLocation != null)
      return _fileLocation;
    return FacesVariableUtil.getString(getValueExpression("fileLocation"));
  }

  public Object saveState(FacesContext context)
  {
    Object[] values = new Object[2];
    values[0] = super.saveState(context);
    values[1] = _fileLocation;
    return values;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object[] values = (Object[]) state;
    super.restoreState(context, values[0]);
    _fileLocation = (String) values[1];
  }

}
