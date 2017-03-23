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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.designer.PluginDataAccess;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.ExternalEvent;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class CopyDialog extends TrayDialog {
    private WorkflowElement workflowElement;
    private WorkflowPackage targetPackage;

    private String originalName;
    private String originalVersion;

    private Text newNameTextField;
    private String newName;

    public String getNewName() {
        return newName;
    }

    public CopyDialog(Shell shell, WorkflowElement workflowElement, String originalName,
            String originalVersion, WorkflowPackage targetPackage) {
        super(shell);
        this.workflowElement = workflowElement;
        this.originalName = originalName;
        this.originalVersion = originalVersion;
        this.targetPackage = targetPackage;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        String message = "";
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.getShell().setText(
                "Copy " + originalName + (originalVersion == null ? "" : " v" + originalVersion));

        new Label(composite, SWT.NONE).setText("New Name:");
        newNameTextField = new Text(composite, SWT.BORDER | SWT.SINGLE);
        GridData gd = new GridData(SWT.LEFT);
        gd.widthHint = 200;
        newNameTextField.setLayoutData(gd);
        newName = getUniqueName(originalName);
        newNameTextField.setText(newName);
        newNameTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String name = newNameTextField.getText().trim();
                if (nameAlreadyExists(name)) {
                    getButton(IDialogConstants.OK_ID).setEnabled(false);
                    WarningTray tray = getWarningTray();
                    tray.setMessage(
                            workflowElement.getTitle() + " name already exists:\n'" + name + "'");
                    tray.open();
                }
                else {
                    newName = name;
                    getWarningTray().close();
                    getButton(IDialogConstants.OK_ID).setEnabled(true);
                }
            }
        });

        new Label(composite, SWT.NONE).setText(message);

        return composite;
    }

    private WarningTray warningTray;

    public WarningTray getWarningTray() {
        if (warningTray == null)
            warningTray = new WarningTray(this);
        return warningTray;
    }

    private boolean nameAlreadyExists(String name) {
        WorkflowProject workflowProject = workflowElement.getProject();
        PluginDataAccess dataAccess = workflowProject.getDataAccess();
        if (workflowElement instanceof WorkflowProcess)
            return dataAccess.processNameExists(targetPackage.getPackageVO(),name);
        else if (workflowElement instanceof ExternalEvent)
            return workflowProject.externalEventNameExists(name);
        else if (workflowElement instanceof WorkflowAsset)
            return targetPackage == null ? workflowProject.workflowAssetNameExists(name)
                    : targetPackage.workflowAssetNameExists(name);
        else
            return false;
    }

    private String getUniqueName(String oldName) {
        String newName = "Copy of " + oldName;
        int idx = 1;
        while (nameAlreadyExists(newName)) {
            idx++;
            newName = "Copy " + idx + " of " + oldName;
        }

        return newName;
    }
}