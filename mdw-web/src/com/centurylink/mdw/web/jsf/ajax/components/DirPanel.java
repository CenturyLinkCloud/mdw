/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.ajax.components;

import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.jsf.dojo.components.DojoComponent;

public class DirPanel extends DojoComponent
{
  public static final String COMPONENT_FAMILY = "com.centurylink.mdw.Ajax";
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.ajax.DirPanel";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.ajax.DirPanelRenderer";

  private String _dataUrl;
  private Boolean _showTimeStamps;

  public DirPanel()
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

  public boolean isShowTimeStamps()
  {
    if (_showTimeStamps != null)
      return _showTimeStamps.booleanValue();
    return FacesVariableUtil.getBoolean(getValueExpression("showTimeStamps"));
  }
  public void setShowTimestamps(boolean showTimeStamps)
  {
    _showTimeStamps = new Boolean(showTimeStamps);
  }


  /**
   * Invoked after the render phase has completed, this method returns an
   * object which can be passed to the restoreState of some other instance of
   * UIComponentBase to reset that object's state to the same values as this
   * object currently has.
   */
  public Object saveState(FacesContext context)
  {
    Object values[] = new Object[3];
    values[0] = super.saveState(context);
    values[1] = _dataUrl;
    values[2] = _showTimeStamps;
    return values;
  }

  /**
   * Invoked in the restore view phase, this method initializes this object's
   * members from the values saved previously into the provided state object.
   *
   * @param state an object previously returned by the saveState method of this class
   */
  public void restoreState(FacesContext context, Object state)
  {
    Object values[] = (Object[]) state;
    super.restoreState(context, values[0]);
    _dataUrl = (String) values[1];
    _showTimeStamps = (Boolean) values[2];
  }


}