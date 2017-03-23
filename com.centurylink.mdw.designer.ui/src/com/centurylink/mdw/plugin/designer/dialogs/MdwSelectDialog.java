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

import java.util.List;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class MdwSelectDialog extends TrayDialog {
    private String message;

    public String getMessage() {
        return message;
    }

    private String selection;

    public String getSelection() {
        return selection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
    }

    private Combo selectCombo;

    private String title = "MDW Select";

    public void setTitle(String title) {
        this.title = title;
    }

    private List<String> options;

    public MdwSelectDialog(Shell shell, String message, List<String> options) {
        super(shell);
        this.message = message;
        this.options = options;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.getShell().setText(title);

        new Label(composite, SWT.NONE).setText(message);

        selectCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (String option : options)
            selectCombo.add(option);

        if (selection != null)
            selectCombo.setText(selection);

        return composite;
    }

    @Override
    protected void cancelPressed() {
        setReturnCode(CANCEL);
        close();
    }

    @Override
    protected void okPressed() {
        // set the input
        selection = selectCombo.getText();
        if (selection.trim().length() == 0)
            selection = null;
        setReturnCode(OK);
        close();
    }
}