/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.convert;

import java.util.ArrayList;
import java.util.List;

public class ListConverter implements ValueConverter
{
  public Object toModelValue(String propertyValue) throws ConversionException
  {
    if (propertyValue == null)
      return null;
    
    List<String> values = new ArrayList<String>();
    for (String s : propertyValue.split("#"))
    {
      if (s.length() > 0) 
        values.add(s);
    }
      
    return values;
  }

  public String toPropertyValue(Object modelValue) throws ConversionException
  {
    if (modelValue == null)
      return null;
    
    if (!(modelValue instanceof List<?>))
      throw new ConversionException("Model value must be an instance of List rather than " + modelValue.getClass().getName());
    
    String value = "";
    List<?> strings = (List<?>) modelValue;
    for (int i = 0; i < strings.size(); i++)
    {
      value += strings.get(i);
      if (i < strings.size() - 1)
        value += "#";
    }
    return value;
  }

}
