/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.components;

public class CheckBox extends DojoComponent
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.dojo.CheckBox";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.dojo.CheckBoxRenderer";

  public CheckBox()
  {
    setRendererType(RENDERER_TYPE);
  }
}