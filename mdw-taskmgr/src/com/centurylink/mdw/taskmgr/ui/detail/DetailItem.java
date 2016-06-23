/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.detail;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;

/**
 * Represents a detail name/value pair. Optionally can support
 * data comparisons based on a old value and a new value;
 */
public class DetailItem extends DataItem
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  
  private String _name;
  public String getName() { return _name; }
  public void setName(String s) { _name = s; }

  private String _attribute;
  public String getAttribute() { return _attribute; }
  public void setAttribute(String s) { _attribute = s; }
  
  private Object _value;
  public Object getValue() { return _value; }
  public void setValue(Object o) { _value = o; }
  
  private Object _oldValue;
  public Object getOldValue() { return _oldValue; }
  public void setOldValue(Object o) { _oldValue = o; }
  
  private boolean _required;
  public boolean isRequired() { return _required; }
  public void setRequired(boolean b) { _required = b; }
  
  private boolean _readOnly;
  public boolean isReadOnly() { return _readOnly; }
  public void setReadOnly(boolean b) { _readOnly = b; }
  
  private String _dataType;
  public String getDataType() { return _dataType; }
  public void setDataType(String s) { _dataType = s; }
  
  private String[] _rolesAllowedToEdit;
  public String[] getRolesAllowedToEdit() { return _rolesAllowedToEdit; }
  public void setRolesAllowedToEdit(String[] sa) { _rolesAllowedToEdit = sa; }

  private String[] _rolesAllowedToView;
  public String[] getRolesAllowedToView() { return _rolesAllowedToView; }
  public void setRolesAllowedToView(String[] sa) { _rolesAllowedToView = sa; }

  public DetailItem(Detail detail, String name, String attribute, Object value)
  {
    setDetail(detail);
    _name = name;
    _attribute = attribute;
    _value = value;
  }
  
  public DetailItem(Detail detail, String name, String attribute, Object oldValue, Object value)
  {
    this(detail, name, attribute, value);
    _oldValue = oldValue;
  }
  
  public boolean isValuePair()
  {
    return _oldValue != null; 
  }
  
  public boolean valueChanged()
  {
    if (_oldValue == null)
      return _value != null;
    else
      return !_oldValue.equals(_value);
  }
  
  public Object getDataValue()
  {
    return getValue();
  }
  
  public void setDataValue(Object value)
  {
    setValue(value);
    ModelWrapper modelWrapper = getDetail().getModelWrapper();
    try
    {
      getDetail().getPropertyUtilsBean().setProperty(modelWrapper, getAttribute(), value);
    }
    catch (Exception ex)
    {
      logger.severeException("Unable to set value for itemAttribute: " + getAttribute(), ex);
    }
  }
  
  public boolean isValueEditable()
  {
    if (isJavaObject())
      return false;
    
    boolean editableAtItemLevel = false;
    if (getRolesAllowedToEdit() != null)
    {    
      for (int i = 0; i < getRolesAllowedToEdit().length; i++)
      {
        if (FacesVariableUtil.getCurrentUser().isInRoleForAnyGroup(getRolesAllowedToEdit()[i]))
          editableAtItemLevel = true;
      }
    }
    
    boolean editableAtDetailLevel = getDetail().isEditable() && !isReadOnly() && !isLink();
    
    return editableAtDetailLevel || editableAtItemLevel;
  }
  
  public boolean isValueVisible()
  {
    if (getRolesAllowedToView() == null)
      return true;
    
    for (String role : getRolesAllowedToView())
    {
      if (FacesVariableUtil.getCurrentUser().isInRoleForAnyGroup(role))
        return true;
    }
    
    return false;
  }

  public boolean isValueRequired()
  {
    return isRequired();
  }
  
  public boolean isRendered()
  {
    if (getDetail().isEditable())
    {
      return isRenderedForEdit();
    }
    else
    {
      return isRenderedForView() && isValueVisible();
    }
  }
}
