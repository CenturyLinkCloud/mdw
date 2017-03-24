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

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class MdwInputDialog extends TrayDialog {
    private String message;

    public String getMessage() {
        return message;
    }

    private String input;

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    private Text inputTextArea;
    private boolean multiLine;

    private String title = "MDW Input";

    public void setTitle(String title) {
        this.title = title;
    }

    private int width = 200;

    public void setWidth(int w) {
        this.width = w;
    }

    private int height;

    public void setHeight(int h) {
        this.height = h;
    }

    public MdwInputDialog(Shell shell, String message, boolean multiLine) {
        super(shell);
        this.message = message;
        this.multiLine = multiLine;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.getShell().setText(title);

        new Label(composite, SWT.NONE).setText(message);

        int style = multiLine ? SWT.MULTI | SWT.BORDER : SWT.BORDER;
        inputTextArea = new Text(composite, style);
        GridData gd = new GridData(GridData.CENTER);
        gd.widthHint = width;
        if (height != 0)
            gd.heightHint = height;
        else if (multiLine)
            gd.heightHint = 40;
        inputTextArea.setLayoutData(gd);

        if (input != null)
            inputTextArea.setText(input);

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
        input = inputTextArea.getText();
        if (input.trim().length() == 0)
            input = null;
        setReturnCode(OK);
        close();
    }
}