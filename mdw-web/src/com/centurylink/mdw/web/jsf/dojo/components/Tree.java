/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.components;

import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class Tree extends DojoComponent
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.dojo.Tree";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.dojo.TreeRenderer";

  private String _source;
  private String _labelAttr;
  private String _getLabel;
  private String _getIconClass;
  private String _selectHandler;

  public Tree()
  {
    setRendererType(RENDERER_TYPE);
  }

  public boolean getRendersChildren()
  {
    return false;
  }

  public String getSource()
  {
    if (_source != null)
      return _source;
    return FacesVariableUtil.getString(getValueExpression("source"));
  }
  public void setSource(String source)
  {
    _source = source;
  }

  public String getLabelAttr()
  {
    if (_labelAttr != null)
      return _labelAttr;
    return FacesVariableUtil.getString(getValueExpression("labelAttr"));
  }
  public void setLabelAttr(String labelAttr)
  {
    _labelAttr = labelAttr;
  }

  public String getGetLabel()
  {
    if (_getLabel != null)
      return _getLabel;
    return FacesVariableUtil.getString(getValueExpression("getLabel"));
  }
  public void setGetLabel(String getLabel)
  {
    _getLabel = getLabel;
  }

  public String getGetIconClass()
  {
    if (_getIconClass != null)
      return _getIconClass;
    return FacesVariableUtil.getString(getValueExpression("getIconClass"));
  }
  public void setGetIconClass(String getIconClass)
  {
    _getIconClass = getIconClass;
  }

  public String getSelectHandler()
  {
    if (_selectHandler != null)
      return _selectHandler;
    return FacesVariableUtil.getString(getValueExpression("selectHandler"));
  }
  public void setSelectHandler(String selectHandler)
  {
    _selectHandler = selectHandler;
  }


  public Object saveState(FacesContext context)
  {
    Object values[] = new Object[6];
    values[0] = super.saveState(context);
    values[1] = _source;
    values[2] = _labelAttr;
    values[3] = _getLabel;
    values[4] = _getIconClass;
    values[5] = _selectHandler;
    return values;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object values[] = (Object[]) state;
    super.restoreState(context, values[0]);
    _source = (String) values[1];
    _labelAttr = (String) values[2];
    _getLabel = (String) values[3];
    _getIconClass = (String) values[4];
    _selectHandler = (String) values[5];
  }
}
