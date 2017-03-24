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

import org.eclipse.jface.dialogs.DialogTray;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class WarningTray extends DialogTray {
    private TrayDialog dialog;

    private String message = "";

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public WarningTray(TrayDialog dialog) {
        this.dialog = dialog;
    }

    public void open() {
        WarningTray warningTray = (WarningTray) dialog.getTray();
        if (warningTray != null)
            dialog.closeTray();

        warningTray = new WarningTray(dialog);
        warningTray.message = this.message;
        dialog.openTray(warningTray);
    }

    public void close() {
        WarningTray warningTray = (WarningTray) dialog.getTray();
        if (warningTray != null) {
            warningTray.setMessage("");
            dialog.closeTray();
        }
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());
        new Label(composite, SWT.NONE).setText(message);
        return composite;
    }
}