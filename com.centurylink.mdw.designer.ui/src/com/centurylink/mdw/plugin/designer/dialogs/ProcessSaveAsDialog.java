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
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.designer.PluginDataAccess;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ProcessSaveAsDialog extends TrayDialog {
    private WorkflowProcess process;

    public WorkflowProcess getProcess() {
        return process;
    }

    private Text newNameTextField;

    private String newName;

    public String getNewName() {
        return newName;
    }

    private Combo workflowPackageCombo;

    private String packageName;

    public String getPackageName() {
        return packageName;
    }

    private WorkflowPackage newPackage;

    public ProcessSaveAsDialog(Shell shell, WorkflowProcess processVersion) {
        super(shell);
        this.process = processVersion;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.getShell().setText("Save Process As…");

        // package selection
        new Label(composite, SWT.NONE).setText("Workflow Package");
        workflowPackageCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData grid = new GridData(SWT.BEGINNING);
        grid.horizontalSpan = 2;
        grid.widthHint = 150;
        workflowPackageCombo.setLayoutData(grid);

        workflowPackageCombo.removeAll();
        for (WorkflowPackage packageVersion : this.process.getProject()
                .getTopLevelUserVisiblePackages())
            workflowPackageCombo.add(packageVersion.getName());

        workflowPackageCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                packageName = workflowPackageCombo.getText().trim();
                if (!StringHelper.isEmpty(packageName))
                    newPackage = getProject().getPackage(packageName);
            }
        });

        if (getProcess().getPackage() != null) {
            packageName = getProcess().getPackage().getName();
            workflowPackageCombo.setText(packageName);
        }

        // process name
        new Label(composite, SWT.NONE).setText("Process Name");
        newNameTextField = new Text(composite, SWT.BORDER | SWT.SINGLE);
        GridData gd = new GridData(SWT.LEFT);
        gd.widthHint = 200;
        newNameTextField.setLayoutData(gd);
        newName = newNameTextField.getText().trim();
        newNameTextField.setText(newName);
        newNameTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String name = newNameTextField.getText().trim();
                String warning = null;
                WorkflowPackage processPkg = newPackage == null ? process.getPackage() : newPackage;
                if (!processPkg.isUserAuthorized(UserRoleVO.ASSET_DESIGN)) {
                    warning = "you are not authorized to create a process in selected workflow Package:\n'"
                            + packageName + "'";
                }
                else if (nameAlreadyExists(name)) {
                    warning = process.getTitle() + " name already exists:\n'" + name + "'";
                }
                if (warning != null) {
                    getButton(IDialogConstants.OK_ID).setEnabled(false);
                    WarningTray tray = getWarningTray();
                    tray.setMessage(warning);
                    tray.open();
                    getButton(Dialog.OK).setEnabled(false);
                }
                else {
                    newName = name;
                    getWarningTray().close();
                    getButton(IDialogConstants.OK_ID).setEnabled(true);
                    getButton(Dialog.OK).setEnabled(name.length() > 0);
                }
            }
        });

        newNameTextField.forceFocus();

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(Dialog.OK).setEnabled(false);
    }

    private WarningTray warningTray;

    public WarningTray getWarningTray() {
        if (warningTray == null)
            warningTray = new WarningTray(this);
        return warningTray;
    }

    private boolean nameAlreadyExists(String name) {
        WorkflowProject workflowProject = process.getProject();
        PluginDataAccess dataAccess = workflowProject.getDataAccess();
        if (process instanceof WorkflowProcess)
            return dataAccess.processNameExists(newPackage.getPackageVO(),name);
        else
            return false;
    }

    public WorkflowProject getProject() {
        return getProcess().getProject();
    }

}