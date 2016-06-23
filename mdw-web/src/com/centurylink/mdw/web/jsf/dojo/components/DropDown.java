/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.components;


public class DropDown extends DojoComponent
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.dojo.DropDown";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.dojo.DropDownRenderer";

  public DropDown()
  {
    setRendererType(RENDERER_TYPE);
  }
}
