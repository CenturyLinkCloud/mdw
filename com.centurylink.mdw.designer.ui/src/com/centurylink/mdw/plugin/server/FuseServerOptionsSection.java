/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

public class FuseServerOptionsSection extends MdwServerOptionsSection
{
  protected String getDescription()
  {
    return "MDW ServiceMix Launch Settings";
  }

  protected String getDefaultJavaOptions()
  {
    return FuseServerBehavior.DEFAULT_JAVA_OPTS;
  }

}
