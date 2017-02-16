/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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