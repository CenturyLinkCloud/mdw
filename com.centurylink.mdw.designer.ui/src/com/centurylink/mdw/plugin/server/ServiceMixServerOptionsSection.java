/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

public class ServiceMixServerOptionsSection extends MdwServerOptionsSection
{
  protected String getDescription()
  {
    return "MDW ServiceMix Launch Settings";
  }

  protected String getDefaultJavaOptions()
  {
    return ServiceMixServerBehavior.DEFAULT_JAVA_OPTS;
  }
}

