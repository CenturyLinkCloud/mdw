/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.convert;


public interface ValueConverter
{
  /**
   * Converts from the value stored on the PropertyEditor
   * to a value that can be used by the model.
   */
  public Object toModelValue(String propertyValue) throws ConversionException;
  
  /**
   * Converts from the value stored on the model to a
   * value that can be associated with a PropertyEditor.
   */
  public String toPropertyValue(Object modelValue) throws ConversionException;
}
