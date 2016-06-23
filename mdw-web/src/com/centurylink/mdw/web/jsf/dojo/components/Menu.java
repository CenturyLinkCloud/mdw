/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.components;


public class Menu extends DojoComponent
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.dojo.Menu";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.dojo.MenuRenderer";

  public Menu()
  {
    setRendererType(RENDERER_TYPE);
  }
}
