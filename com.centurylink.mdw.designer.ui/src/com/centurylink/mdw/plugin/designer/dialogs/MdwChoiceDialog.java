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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class MdwChoiceDialog extends TrayDialog {
    public static final int CANCEL = -1;

    private String message;

    public String getMessage() {
        return message;
    }

    private String title = "MDW Choose";

    public void setTitle(String title) {
        this.title = title;
    }

    private String[] choices;

    public String[] getChoices() {
        return choices;
    }

    private int choice;

    public int getChoice() {
        return choice;
    }

    private Button[] choiceButtons;

    public MdwChoiceDialog(Shell shell, String message, String[] choices) {
        super(shell);
        this.message = message;
        this.choices = choices;
        this.choiceButtons = new Button[choices.length];
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.getShell().setText(title);

        new Label(composite, SWT.NONE).setText(message);

        Group radioGroup = new Group(composite, SWT.NONE);
        // radioGroup.setText(message);
        GridLayout gl = new GridLayout();
        radioGroup.setLayout(gl);
        GridData gd = new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        radioGroup.setLayoutData(gd);
        for (int i = 0; i < choices.length; i++) {
            choiceButtons[i] = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
            choiceButtons[i].setText(choices[i]);
        }

        return composite;
    }

    @Override
    protected void cancelPressed() {
        setReturnCode(CANCEL);
        close();
    }

    @Override
    protected void okPressed() {
        // find the choice
        for (int i = 0; i < choices.length; i++) {
            if (choiceButtons[i].getSelection())
                setReturnCode(i);
        }
        close();
    }
}