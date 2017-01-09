/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.rcp;

import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class MdwWorkbenchAdvisor extends WorkbenchAdvisor
{
  private static final String PERSPECTIVE_ID = "MDWDesignerRCP.perspective";

  public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer)
  {
    return new MdwWorkbenchWindowAdvisor(configurer);
  }

  public String getInitialWindowPerspectiveId()
  {
    return PERSPECTIVE_ID;
  }
  
  @Override
  public void initialize(IWorkbenchConfigurer configurer)
  {
    super.initialize(configurer);
    configurer.setSaveAndRestore(true);
  }
  
}
