/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.converters;

import java.util.Calendar;
import java.util.Date;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;

/**
 * This is only used where it is specifically applied via the "converter" attribute.
 * The default converter is DefaultDateTimeConverter.
 */
public class DateTimeConverter extends DefaultDateTimeConverter
{
  public Object getAsObject(FacesContext facesContext, UIComponent component, String value) throws ConverterException
  {
    if (value == null || value.trim().length() == 0)
      return null;
        
    Calendar cal = Calendar.getInstance();
    
    Date date = (Date) super.getAsObject(facesContext, component, value);
    cal.setTime(date);
    
    // if user typed '07' for yyyy, convert to 2007
    if (cal.get(Calendar.YEAR) < 100)
      cal.add(Calendar.YEAR, 2000);

    Object converted = component.getAttributes().get("converted");
    if (converted != null && !Boolean.valueOf(converted.toString()))
      return super.getAsString(facesContext, component, cal.getTime());

    return cal.getTime();
  }
}
