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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ValueDisplayDialog extends TrayDialog {
    private String title;
    private String value;
    private boolean createCancelButton;

    public ValueDisplayDialog(Shell shell, String value) {
        this(shell, value, "Value", false);
    }

    public ValueDisplayDialog(Shell shell, String title, String value, boolean cancelButton) {
        super(shell);
        this.title = title;
        this.value = value;
        this.createCancelButton = cancelButton;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        composite.setLayout(layout);
        composite.getShell().setText(title);

        // value
        Text valueText = new Text(composite, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 500;
        gd.heightHint = 400;
        valueText.setLayoutData(gd);
        if (value != null)
            valueText.setText(value);

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
                true);
        okButton.forceFocus();

        if (createCancelButton)
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

}