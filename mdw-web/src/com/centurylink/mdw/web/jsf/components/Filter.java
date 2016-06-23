/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.components;

import javax.faces.component.UIData;
import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;


/**
 * Filter component for a sortable list.
 */
public class Filter extends UIData
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.web.jsf.components.Filter";

  // var, value, id are handled by UIData

  private String _name;
  public void setName(String s) { _name = s; }
  public String getName()
  {
    if (_name != null)
      return _name;
    return FacesVariableUtil.getString(getValueExpression("name"));
  }

  private String _filterAction;
  public void setFilterAction(String s) { _filterAction = s; }
  public String getFilterAction()
  {
    if (_filterAction != null)
      return _filterAction;
    return FacesVariableUtil.getString(getValueExpression("filterAction"));
  }

  private String _actionListener;
  public void setActionListener(String s) { _actionListener = s; }
  public String getActionListener()
  {
    if (_actionListener != null)
      return _actionListener;
    return FacesVariableUtil.getString(getValueExpression("actionListener"));
  }

  private String _width;
  public void setWidth(String s) { _width = s; }
  public String getWidth()
  {
    if (_width != null)
      return _width;
    return FacesVariableUtil.getString(getValueExpression("width"));
  }

  private String _headerClass;
  public void setHeaderClass(String s) { _headerClass = s; }
  public String getHeaderClass()
  {
    if (_headerClass != null)
      return _headerClass;
    return FacesVariableUtil.getString(getValueExpression("headerClass"));
  }

  private Boolean _showHeader;
  public void setShowHeader(boolean showHeader)
  {
    _showHeader = new Boolean(showHeader);
  }
  public boolean isShowHeader()
  {
    if (_showHeader != null)
      return _showHeader.booleanValue();

    return FacesVariableUtil.getBoolean(getValueExpression("showHeader"), true);
  }

  private String _footerClass;
  public void setFooterClass(String s) { _footerClass = s; }
  public String getFooterClass()
  {
    if (_footerClass != null)
      return _footerClass;
    return FacesVariableUtil.getString(getValueExpression("footerClass"));
  }

  private String _labelRowClass;
  public void setLabelRowClass(String s) { _labelRowClass = s; }
  public String getLabelRowClass()
  {
    if (_labelRowClass != null)
      return _labelRowClass;
    return FacesVariableUtil.getString(getValueExpression("labelRowClass"));
  }

  private String _inputRowClass;
  public void setInputRowClass(String s) { _inputRowClass = s; }
  public String getInputRowClass()
  {
    if (_inputRowClass != null)
      return _inputRowClass;
    return FacesVariableUtil.getString(getValueExpression("inputRowClass"));
  }

  private String _submitButtonLabel;
  public void setSubmitButtonLabel(String s) { _submitButtonLabel = s; }
  public String getSubmitButtonLabel()
  {
    if (_submitButtonLabel != null)
      return _submitButtonLabel;
    return FacesVariableUtil.getString(getValueExpression("submitButtonLabel"));
  }

  private String _resetButtonLabel;
  public void setResetButtonLabel(String s) { _resetButtonLabel = s; }
  public String getResetButtonLabel()
  {
    if (_resetButtonLabel != null)
      return _resetButtonLabel;
    return FacesVariableUtil.getString(getValueExpression("resetButtonLabel"));
  }

  private Boolean _swapButtonPosition;
  public void setSwapButtonPosition(boolean swapButtonPosition)
  {
    _swapButtonPosition = new Boolean(swapButtonPosition);
  }
  public boolean isSwapButtonPosition()
  {
    if (_swapButtonPosition != null)
      return _swapButtonPosition.booleanValue();
    return FacesVariableUtil.getBoolean(getValueExpression("swapButtonPosition"));
  }
  public boolean hasSwapButtonPositionValue()
  {
    if (_swapButtonPosition != null)
      return true;
    return getValueExpression("swapButtonPosition") != null;
  }

  private Boolean _showCommandButtons;
  public void setShowCommandButtons(boolean showCommandButtons)
  {
    _showCommandButtons = new Boolean(showCommandButtons);
  }
  public boolean isShowCommandButtons()
  {
    if (_showCommandButtons != null)
      return _showCommandButtons.booleanValue();
    return FacesVariableUtil.getBoolean(getValueExpression("showCommandButtons"), true);
  }

  private Boolean _showPrefsButton;
  public void setShowPrefsButton(boolean showPrefsButton)
  {
    _showPrefsButton = new Boolean(showPrefsButton);
  }
  public boolean isShowPrefsButton()
  {
    if (_showPrefsButton != null)
      return _showPrefsButton.booleanValue();
    return FacesVariableUtil.getBoolean(getValueExpression("showPrefsButton"), true);
  }

  private String _commandButtonClass;
  public void setCommandButtonClass(String s) { _commandButtonClass = s; }
  public String getCommandButtonClass()
  {
    if (_commandButtonClass != null)
      return _commandButtonClass;
    return FacesVariableUtil.getString(getValueExpression("commandButtonClass"));
  }

  private String _inputTdHeight;
  public void setInputTdHeight(String s) { _inputTdHeight = s; }
  public String getInputTdHeight()
  {
    if (_inputTdHeight != null)
      return _inputTdHeight;
    return FacesVariableUtil.getString(getValueExpression("inputTdHeight"));
  }

  public Object saveState(FacesContext context)
  {
    Object[] values = new Object[17];
    values[0] = super.saveState(context);
    values[1] = _name;
    values[2] = _filterAction;
    values[3] = _width;
    values[4] = _headerClass;
    values[5] = _labelRowClass;
    values[6] = _inputRowClass;
    values[7] = _submitButtonLabel;
    values[8] = _resetButtonLabel;
    values[9] = _commandButtonClass;
    values[10] = _footerClass;
    values[11] = _inputTdHeight;
    values[12] = _swapButtonPosition;
    values[13] = _showHeader;
    values[14] = _showCommandButtons;
    values[15] = _showPrefsButton;
    values[16] = _actionListener;
    return values;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object[] values = (Object[]) state;
    super.restoreState(context, values[0]);
    _name = (String) values[1];
    _filterAction = (String) values[2];
    _width = (String) values[3];
    _headerClass = (String) values[4];
    _labelRowClass = (String) values[5];
    _inputRowClass = (String) values[6];
    _submitButtonLabel = (String) values[7];
    _resetButtonLabel = (String) values[8];
    _commandButtonClass = (String) values[9];
    _footerClass = (String) values[10];
    _inputTdHeight = (String) values[11];
    _swapButtonPosition = (Boolean) values[12];
    _showHeader = (Boolean) values[13];
    _showCommandButtons = (Boolean) values[14];
    _showPrefsButton = (Boolean) values[15];
    _actionListener = (String) values[16];
  }

  public void addFilterActionListener(FilterActionListener listener)
  {
    addFacesListener(listener);
  }

  public void removeFilterActionListener(FilterActionListener listener)
  {
    removeFacesListener(listener);
  }
}
