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
package com.centurylink.mdw.plugin.designer.dialogs;

import java.awt.Component;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.designer.utils.CustomOptionPane;

public class SwtDialogProvider implements CustomOptionPane {
    private Display display;

    public SwtDialogProvider(Display display) {
        this.display = display;
    }

    public void showMessage(Component parent, final String message) {
        display.syncExec(new Runnable() {
            public void run() {
                PluginMessages.uiMessage(getShell(), "MDW Message", message, null,
                        PluginMessages.INFO_MESSAGE);
            }
        });
    }

    public void showError(Component parent, final String message) {
        // don't annoy users with this popup
        if (message != null && message.startsWith("This is readonly"))
            return;

        display.syncExec(new Runnable() {
            public void run() {
                PluginMessages.uiMessage(getShell(), message, "MDW Error", null,
                        PluginMessages.ERROR_MESSAGE);
            }
        });
    }

    private boolean dispensation;

    public boolean confirm(Component parent, final String message, final boolean yes_no) {
        display.syncExec(new Runnable() {
            public void run() {
                dispensation = MessageDialog.openConfirm(getShell(), "MDW Confirm", message);
            }
        });

        return dispensation;
    }

    private int choice;

    public int choose(Component parent, final String message, final String[] choices) {
        display.syncExec(new Runnable() {
            public void run() {
                MdwChoiceDialog choiceDlg = new MdwChoiceDialog(getShell(), message, choices);
                choice = choiceDlg.open();
            }
        });

        return choice;
    }

    String input;

    public String getInput(Component parent, final String message) {
        display.syncExec(new Runnable() {
            public void run() {
                MdwInputDialog inputDlg = new MdwInputDialog(getShell(), message, true);
                int result = inputDlg.open();
                input = result == 1 ? null : inputDlg.getInput();
            }
        });

        return input;
    }

    private Shell getShell() {
        return MdwPlugin.getActiveWorkbenchWindow().getShell();
    }

}
