/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.validators;

import java.util.Date;
import java.util.Iterator;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.web.jsf.converters.DateTimeConverter;

public class DateRangeValidator implements Validator
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  
  private static final String MESSAGE = "'From Date' cannot be later than 'To Date'";
  
  public void validate(FacesContext context, UIComponent component, Object value)
      throws ValidatorException
  {
    Date fromDate = null;
    Date toDate = null;    
    DateTimeConverter converter = new DateTimeConverter();
    converter.setPattern("MM/dd/yyyy");
    
    if (component.getId().equals("fromDate"))
    {
      fromDate = (Date) value;
      HtmlInputText toDateComponent = (HtmlInputText) component.findComponent("toDate");
      try
      {
        toDate = (Date) converter.getAsObject(context, toDateComponent, (String)toDateComponent.getSubmittedValue());
      }
      catch (ConverterException ex)
      {
        logger.severeException(ex.getMessage(), ex);
        return;  // let the date format validator report parsing exceptions
      }
    }
    if (component.getId().equals("toDate"))
    {
      toDate = (Date) value;
      HtmlInputText fromDateComponent = (HtmlInputText) component.findComponent("fromDate");
      try
      {
        fromDate = (Date) converter.getAsObject(context, fromDateComponent, (String)fromDateComponent.getSubmittedValue());
      }
      catch (ConverterException ex)
      {
        logger.severeException(ex.getMessage(), ex);
        return;  // let the date format validator report parsing exceptions
      }
    }
    
    if (fromDate == null || toDate == null)
      return;
    
    if (fromDate.compareTo(toDate) > 0)
    {
      // don't add duplicate global messages
      for (Iterator<FacesMessage> iter = context.getMessages(); iter.hasNext(); )
      {
        FacesMessage existing = iter.next();
        if (existing.getSummary() != null && existing.getSummary().equals(MESSAGE))
          return;
      }
      FacesMessage message = new FacesMessage(MESSAGE, "");
      context.addMessage(null, message);
      throw new ValidatorException(message);
    }
  }
  
}
