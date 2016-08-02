/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.rcp;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * Controls all aspects of the application's execution
 */
public class MdwApplication implements IApplication
{
  public Object start(IApplicationContext context) throws Exception
  {
    Display display = PlatformUI.createDisplay();
    try
    {
      int returnCode = PlatformUI.createAndRunWorkbench(display, new MdwWorkbenchAdvisor());
      
      if (returnCode == PlatformUI.RETURN_RESTART)
        return IApplication.EXIT_RESTART;
      else
        return IApplication.EXIT_OK;
    }
    finally
    {
      display.dispose();
    }

  }

  public void stop()
  {
    final IWorkbench workbench = PlatformUI.getWorkbench();
    if (workbench == null)
      return;
    final Display display = workbench.getDisplay();
    display.syncExec(new Runnable()
    {
      public void run()
      {
        if (!display.isDisposed())
          workbench.close();
      }
    });
  }
}
