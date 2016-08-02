/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.convert;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateConverter implements ValueConverter
{
  SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy  HH:mm:ss");
  
  public Object toModelValue(String propertyValue) throws ConversionException
  {
    try
    {
      return dateFormat.parse(propertyValue);
    }
    catch (ParseException ex)
    {
      throw new ConversionException(ex.getMessage(), ex);
    }
  }

  public String toPropertyValue(Object modelValue) throws ConversionException
  {
    if (!(modelValue instanceof Date))
      throw new ConversionException("Must be instance of java.util.Date instead of " + modelValue.getClass().getName());
    return dateFormat.format((Date)modelValue);
  }

}
