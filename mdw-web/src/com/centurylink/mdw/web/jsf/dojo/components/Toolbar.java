/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.components;

public class Toolbar extends DojoLayout
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.dojo.Toolbar";
  public static final String RENDERER_TYPE = "com.centurylink.mdw.dojo.ToolbarRenderer";

  public Toolbar()
  {
    setRendererType(RENDERER_TYPE);
  }
}