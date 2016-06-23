/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.convert;

import com.qwest.mbeng.MbengException;
import com.centurylink.mdw.model.value.event.BamMessageDefinition;

public class BamMessageDefValueConverter implements ValueConverter
{
  public Object toModelValue(String propertyValue) throws ConversionException
  {
    if (propertyValue == null || propertyValue.isEmpty())
      return null;

    try
    {
      return new BamMessageDefinition(propertyValue);
    }
    catch (MbengException ex)
    {
      throw new ConversionException(ex.getMessage(), ex);
    }
  }

  public String toPropertyValue(Object modelValue) throws ConversionException
  {
    if (!(modelValue instanceof BamMessageDefinition))
      throw new ConversionException("Must be instance of BamMessageDefinition instead of " + modelValue.getClass().getName());

    BamMessageDefinition bamMsgDef = (BamMessageDefinition) modelValue;

    try
    {
      return bamMsgDef.format();
    }
    catch (MbengException ex)
    {
      throw new ConversionException("Conversion error for BAM Message: " + modelValue, ex);
    }
  }
}
