/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.designer.testing.StubServer.Stubber;
import com.centurylink.mdw.plugin.server.StubServerDialog;

/**
 * Stubber implementation that pops up a dialog for stubbed responses.
 */
public class UiStubber implements Stubber
{
  public String processMessage(final String masterRequestId, final String request)
  {
    final IWorkbench workbench = PlatformUI.getWorkbench();
    final StringBuffer result = new StringBuffer();
    workbench.getDisplay().syncExec(new Runnable()
    {
      public void run()
      {
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        if (window != null)
        {
          StubServerDialog dialog = new StubServerDialog(window.getShell(), masterRequestId, request);
          int res = dialog.open();
          if (res == StubServerDialog.SUBMIT)
            result.append(dialog.getResponse());
          else if (res == StubServerDialog.PASSTHROUGH)
            result.append(AdapterActivity.MAKE_ACTUAL_CALL);
          else if (res == StubServerDialog.CANCEL)
            result.append("cancel");
        }
      }
    });
    return result.toString();
  }
}
