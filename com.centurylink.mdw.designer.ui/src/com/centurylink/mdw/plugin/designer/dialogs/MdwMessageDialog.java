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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.plugin.PluginMessages;

public class MdwMessageDialog extends MessageDialog {
    private int messageLevel;
    private boolean offerSend;

    private boolean reportMessage;

    public boolean isReportMessage() {
        return reportMessage;
    }

    private Button reportMessageCheckbox;

    public MdwMessageDialog(Shell shell, String title, String message, int level) {
        super(shell, title, null, message, MessageDialog.ERROR,
                new String[] { IDialogConstants.OK_LABEL }, 0);
        this.messageLevel = level;
        this.reportMessage = level >= PluginMessages.getReportingLevel();
        this.offerSend = reportMessage;
    }

    @Override
    public Image getImage() {
        if (messageLevel <= PluginMessages.INFO_MESSAGE)
            return getInfoImage();
        else if (messageLevel <= PluginMessages.WARNING_MESSAGE)
            return this.getWarningImage();
        else
            return this.getErrorImage();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        if (offerSend) {
            reportMessageCheckbox = new Button(composite, SWT.CHECK);
            reportMessageCheckbox.setText("Report this to MDW support");
            reportMessageCheckbox.setSelection(reportMessage);
            GridData gd = new GridData(
                    GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
            gd.horizontalIndent = 10;
            reportMessageCheckbox.setLayoutData(gd);
        }

        return composite;
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == Dialog.OK) {
            if (offerSend)
                reportMessage = reportMessageCheckbox.getSelection();
            close();
        }
    }

    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "OK", true);
    }
}