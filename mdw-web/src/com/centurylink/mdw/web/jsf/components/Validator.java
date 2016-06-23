/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.components;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class Validator extends UIOutput
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.web.jsf.components.Validator";

  private String _validatorId;
  public void setValidatorId(String s) { _validatorId = s; }
  public String getValidatorId()
  {
    if (_validatorId != null)
      return _validatorId;
    return FacesVariableUtil.getString(getValueExpression("validatorId"));
  }

  private String _sequenceId;
  public void setSequenceId(String s) { _sequenceId = s; }
  public String getSequenceId()
  {
    if (_sequenceId != null)
      return _sequenceId;
    return FacesVariableUtil.getString(getValueExpression("sequenceId"));
  }

  public Object saveState(FacesContext context)
  {
    Object[] values = new Object[3];
    values[0] = super.saveState(context);
    values[1] = _validatorId;
    values[2] = _sequenceId;
    return values;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object[] values = (Object[]) state;
    super.restoreState(context, values[0]);
    _validatorId = (String) values[1];
    _sequenceId = (String) values[2];
  }
}
