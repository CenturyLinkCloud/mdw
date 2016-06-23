/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.components;

import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class AccordionPane extends DojoComponent
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.dojo.AccordionPane";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.dojo.AccordionPaneRenderer";

  private String _title;
  private Boolean _selected;

  public AccordionPane()
  {
    setRendererType(RENDERER_TYPE);
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

  public boolean isSelected()
  {
    if (_selected != null)
      return _selected.booleanValue();
    return FacesVariableUtil.getBoolean(getValueExpression("selected"));
  }
  public void setSelected(boolean selected)
  {
    _selected = new Boolean(selected);
  }


  public Object saveState(FacesContext context)
  {
    Object values[] = new Object[3];
    values[0] = super.saveState(context);
    values[1] = _title;
    values[2] = _selected;
    return values;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object values[] = (Object[]) state;
    super.restoreState(context, values[0]);
    _title = (String) values[1];
    _selected = (Boolean) values[2];
  }

}
