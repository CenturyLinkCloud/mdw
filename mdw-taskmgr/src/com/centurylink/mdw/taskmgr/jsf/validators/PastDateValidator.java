/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.jsf.validators;

import java.util.Calendar;
import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

public class PastDateValidator implements Validator
{
  public void validate(FacesContext facesContext, UIComponent uIComponent, Object pDate) throws ValidatorException
  {
    if (compareTo(Calendar.getInstance().getTime(), (Date) pDate, true) > 0)
    {
      Calendar cal = Calendar.getInstance();
      cal.setTime((Date) pDate);
      FacesMessage message = new FacesMessage();
      message.setDetail("Date can not be in the past. " + (cal.get(Calendar.MONTH) + 1) + "/"
          + cal.get(Calendar.DATE) + "/" + cal.get(Calendar.YEAR));
      message.setSummary("Date in the past.");
      message.setSeverity(FacesMessage.SEVERITY_ERROR);
      throw new ValidatorException(message);
    }
  }

  public int compareTo(Date pRefDate, Date pCmpDate, boolean pIgnoreTime)
  {
    if (!pIgnoreTime)
      return pRefDate.compareTo(pCmpDate);

    Calendar cal1 = Calendar.getInstance();
    cal1.clear();
    Calendar cal2 = Calendar.getInstance();
    cal2.setTime(pRefDate);
    cal1.set(cal2.get(Calendar.YEAR), cal2.get(Calendar.MONTH), cal2.get(Calendar.DATE));
    pRefDate = cal1.getTime();

    cal1.clear();
    cal2.setTime(pCmpDate);
    cal1.set(cal2.get(Calendar.YEAR), cal2.get(Calendar.MONTH), cal2.get(Calendar.DATE));
    pCmpDate = cal1.getTime();

    return pRefDate.compareTo(pCmpDate);
  }
}