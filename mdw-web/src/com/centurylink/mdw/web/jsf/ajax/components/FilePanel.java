/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.ajax.components;

import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.jsf.dojo.components.DojoComponent;

public class FilePanel extends DojoComponent
{
  public static final String COMPONENT_FAMILY = "com.centurylink.mdw.Ajax";
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.ajax.FilePanel";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.ajax.FilePanelRenderer";

  private String _filePath;
  private Integer _bufferLines;
  private Integer _refetchThreshold;
  private Boolean _escape;
  private String _fontSize;
  private Integer _tailInterval;
  private Integer _sliderIncrementLines;
  private Boolean _systemAdminUser;
  private Boolean _standAloneMode;

  public FilePanel()
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

  public String getFilePath()
  {
    if (_filePath != null)
      return _filePath;
    return FacesVariableUtil.getString(getValueExpression("filePath"));
  }
  public void setFilePath(String filePath)
  {
    _filePath = filePath;
  }

  public int getBufferLines()
  {
    if (_bufferLines != null)
      return _bufferLines.intValue();
    return FacesVariableUtil.getInt(getValueExpression("bufferLines"), 100);
  }
  public void setBufferLines(int bufferLines)
  {
    _bufferLines = new Integer(bufferLines);
  }

  public int getRefetchThreshold()
  {
    if (_refetchThreshold != null)
      return _refetchThreshold.intValue();
    return FacesVariableUtil.getInt(getValueExpression("refetchThreshold"), 10);
  }
  public void setRefetchThreshold(int refetchThreshold)
  {
    _refetchThreshold = new Integer(refetchThreshold);
  }

  public boolean isEscape()
  {
    if (_escape != null)
      return _escape.booleanValue();
    return FacesVariableUtil.getBoolean(getValueExpression("escape"));
  }
  public void setEscape(boolean escape)
  {
    _escape = new Boolean(escape);
  }

  public String getFontSize()
  {
    if (_fontSize != null)
      return _fontSize;
    String value = FacesVariableUtil.getString(getValueExpression("fontSize"));
    return value == null ? "10pt" : value;
  }
  public void setFontSize(String fontSize)
  {
    _fontSize = fontSize;
  }

  public int getTailInterval()
  {
    if (_tailInterval != null)
      return _tailInterval.intValue();
    return FacesVariableUtil.getInt(getValueExpression("tailInterval"), 10);
  }
  public void setTailInterval(int tailInterval)
  {
    _tailInterval = new Integer(tailInterval);
  }

  public int getSliderIncrementLines()
  {
    if (_sliderIncrementLines != null)
      return _sliderIncrementLines.intValue();
    return FacesVariableUtil.getInt(getValueExpression("sliderIncrementLines"), 5);
  }
  public void setSliderIncrementLines(int sliderIncrementLines)
  {
    _sliderIncrementLines = new Integer(sliderIncrementLines);
  }

  public boolean isSystemAdminUser()
  {
    if (_systemAdminUser != null)
      return _systemAdminUser.booleanValue();
    return FacesVariableUtil.getBoolean(getValueExpression("systemAdminUser"));
  }
  public void setSystemAdminUser(boolean systemAdminUser)
  {
    _systemAdminUser = new Boolean(systemAdminUser);
  }

  public boolean isStandAloneMode()
  {
    if (_standAloneMode != null)
      return _standAloneMode.booleanValue();
    return FacesVariableUtil.getBoolean(getValueExpression("standAloneMode"));
  }
  public void setStandAloneMode(boolean standAloneMode)
  {
    _standAloneMode = new Boolean(standAloneMode);
  }

  /**
   * Invoked after the render phase has completed, this method returns an
   * object which can be passed to the restoreState of some other instance of
   * UIComponentBase to reset that object's state to the same values as this
   * object currently has.
   */
  public Object saveState(FacesContext context)
  {
    Object values[] = new Object[10];
    values[0] = super.saveState(context);
    values[1] = _filePath;
    values[2] = _bufferLines;
    values[3] = _refetchThreshold;
    values[4] = _escape;
    values[5] = _fontSize;
    values[6] = _tailInterval;
    values[7] = _sliderIncrementLines;
    values[8] = _systemAdminUser;
    values[9] = _standAloneMode;
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
    _filePath = (String) values[1];
    _bufferLines = (Integer) values[2];
    _refetchThreshold = (Integer) values[3];
    _escape = (Boolean) values[4];
    _fontSize = (String) values[5];
    _tailInterval = (Integer) values[6];
    _sliderIncrementLines = (Integer) values[7];
    _systemAdminUser = (Boolean) values[8];
    _standAloneMode = (Boolean) values[9];
  }
}