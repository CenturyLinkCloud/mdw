/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.validators;

import java.io.Serializable;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

public class DataItemValidator implements javax.faces.validator.Validator, Serializable
{
  private static final long serialVersionUID = 1L;
  
  private String _dataItemSequenceId;
  public String getDataItemSequenceId() { return _dataItemSequenceId; }
  public void setDataItemSequenceId(String s) { _dataItemSequenceId = s; }
  
  public void validate(FacesContext facesContext, UIComponent component, Object value)
      throws ValidatorException
  {
    // only perform validation if the sequence number indicates applicability
    ValueExpression ve = component.getValueExpression("sequenceId");
    if (ve != null && ve.getValue(facesContext.getELContext()).toString().equals(_dataItemSequenceId))
    {
      validateDataItem(facesContext, component, value);
    }
  }
  
  /**
   * Implements the validation logic for the value of the data item.
   * Default logic is determined by the dataType attribute.
   * @param facesContext faces context
   * @param component the component whose value is being validated
   * @param value the potential new value of the component
   */
  public void validateDataItem(FacesContext facesContext, UIComponent component, Object value)
    throws ValidatorException
  {
    ValueExpression ve = component.getValueExpression("dataType");
    if (ve != null && value != null)
    {
      ELContext elContext = facesContext.getELContext();
      Object dataType = ve.getValue(elContext) == null ? null : ve.getValue(elContext).toString();
        if ("java.lang.Integer[]".equals(dataType))
        {
          try
          {
            Integer.parseInt(value.toString());
          }
          catch (NumberFormatException ex)
          {
            String msg = "Invalid numeric value";
            FacesMessage facesMessage = new FacesMessage(component.getClientId(facesContext), msg);
            throw new ValidatorException(facesMessage, ex);
          }
        }
        else if ("java.lang.Long[]".equals(dataType))
        {
          try
          {
            Long.parseLong(value.toString());
          }
          catch (NumberFormatException ex)
          {
            String msg = "Invalid numeric value";
            FacesMessage facesMessage = new FacesMessage(component.getClientId(facesContext), msg);
            throw new ValidatorException(facesMessage, ex);
          }
        }
    }
  }
  
  /**
   * Override to implement dynamic logic for determining whether a value is required.
   * @param facesContext faces context
   * @param component the component whose value is being validated
   * @return true if the dataItem value is required
   */
  public boolean isRequired(FacesContext facesContext, UIComponent component)
  {
    return false;
  }
}