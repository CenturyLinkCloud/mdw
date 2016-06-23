/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui.input;


public class TnInput extends Input
{
  public TnInput(String attribute, String label)
  {
    super(attribute, label);
  }
  
  /**
   * strip hyphen characters
   */
  public Object getValue()
  {
    String value = (String) super.getValue();
    if (value == null)
      return null;
      
    return value.replaceAll("-", "");
  }

}
