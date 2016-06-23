/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.jsf.converters;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

public class StringDataConverter implements Converter
{
  public Object getAsObject(FacesContext context, UIComponent component, String value)
      throws ConverterException
  {
    return value;
  }

  public String getAsString(FacesContext context, UIComponent component, Object value)
      throws ConverterException
  {
    if (value == null)
      return null;
    
    return value.toString();
  }

}
