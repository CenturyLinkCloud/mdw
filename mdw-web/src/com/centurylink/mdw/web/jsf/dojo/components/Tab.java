/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.components;

import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class Tab extends DojoLayout
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.dojo.Tab";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.dojo.TabRenderer";

  private String _title;
  private Boolean _selected;
  private Boolean _closable;

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

  public boolean isClosable()
  {
    if (_closable != null)
      return _closable.booleanValue();
    return FacesVariableUtil.getBoolean(getValueExpression("closable"));
  }
  public void setClosable(boolean closable)
  {
    _closable = new Boolean(closable);
  }

  public Object saveState(FacesContext context)
  {
    Object values[] = new Object[4];
    values[0] = super.saveState(context);
    values[1] = _title;
    values[2] = _selected;
    values[3] = _closable;
    return values;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object values[] = (Object[]) state;
    super.restoreState(context, values[0]);
    _title = (String) values[1];
    _selected = (Boolean) values[2];
    _closable = (Boolean) values[3];
  }
}
