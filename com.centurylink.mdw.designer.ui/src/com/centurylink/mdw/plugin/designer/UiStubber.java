/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public class UiStubber implements Stubber {
    public String processMessage(final String masterRequestId, final String request) {
        final IWorkbench workbench = PlatformUI.getWorkbench();
        final StringBuffer result = new StringBuffer();
        workbench.getDisplay().syncExec(new Runnable() {
            public void run() {
                IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
                if (window != null) {
                    StubServerDialog dialog = new StubServerDialog(window.getShell(),
                            masterRequestId, request);
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
