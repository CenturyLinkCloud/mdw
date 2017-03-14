/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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

import com.centurylink.mdw.java.JavaNaming;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class RenameDialog extends TrayDialog {
    private Text newNameTextField;
    private WorkflowElement toRename;
    private WorkflowProject workflowProject;

    private String newName;

    public String getNewName() {
        return newName;
    }

    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public RenameDialog(Shell shell, WorkflowElement toRename) {
        super(shell);
        this.toRename = toRename;
        workflowProject = toRename.getProject();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        String message = "";
        final WorkflowProcess  processVersion;
        Composite composite = (Composite) super.createDialogArea(parent);
        if (toRename instanceof WorkflowProcess) {
            processVersion = (WorkflowProcess) toRename;
            if (processVersion.hasDescendantProcessVersions())
                message = "Renaming all process versions.";
            else
                message = "Renamed process will be version 1.";

            message += "\nCalling processes must be updated manually.";
        }else{
            processVersion = null;
        }
        composite.getShell().setText(title);

        new Label(composite, SWT.NONE).setText("New Name:");
        newNameTextField = new Text(composite, SWT.BORDER | SWT.SINGLE);
        GridData gd = new GridData(SWT.LEFT);
        gd.widthHint = 200;
        newNameTextField.setLayoutData(gd);
        newNameTextField.setText(toRename.getName());
        newNameTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String name = newNameTextField.getText().trim();
                if (name.length() == 0) {
                    getButton(IDialogConstants.OK_ID).setEnabled(false);
                    return;
                }

                String error = null;
                if (toRename instanceof WorkflowProcess
                        &&  workflowProject.getDataAccess().processNameExists(processVersion.getPackage().getPackageVO(),name))
                    error = "Process name already exists:\n'" + name + "'";
                else if (toRename instanceof WorkflowPackage) {
                    WorkflowPackage packageVersion = (WorkflowPackage) toRename;
                    if (workflowProject.packageNameExists(name)) {
                        error = "Package name already exists:\n'" + name + "'";
                    }
                    else {
                        boolean containsJavaOrGroovy = false;
                        for (WorkflowAsset asset : packageVersion.getAssets()) {
                            if (RuleSetVO.GROOVY.equals(asset.getLanguage())
                                    || RuleSetVO.JAVA.equals(asset.getLanguage())) {
                                containsJavaOrGroovy = true;
                                break;
                            }
                        }
                        if (containsJavaOrGroovy && !"true"
                                .equals(System.getProperty("mdw.allow.nonstandard.naming"))) {
                            String goodPkgName = JavaNaming.getValidPackageName(name);
                            if (!goodPkgName.equals(name))
                                error = "Packages with Java or Groovy assets must comply with Java package naming restrictions.";
                        }
                    }
                }

                if (error != null) {
                    getButton(IDialogConstants.OK_ID).setEnabled(false);
                    WarningTray tray = getWarningTray();
                    tray.setMessage(error);
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
}