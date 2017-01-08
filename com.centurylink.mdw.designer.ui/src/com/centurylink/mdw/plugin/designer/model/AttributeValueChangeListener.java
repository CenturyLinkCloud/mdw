/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

public abstract class AttributeValueChangeListener
{
  private String attributeName;
  public String getAttributeName() { return attributeName; }
  
  public AttributeValueChangeListener(String attributeName)
  {
    if (attributeName == null)
      throw new NullPointerException("Attribute name cannot be null");
    
    this.attributeName = attributeName;
  }
  
  public abstract void attributeValueChanged(String newValue);
}
