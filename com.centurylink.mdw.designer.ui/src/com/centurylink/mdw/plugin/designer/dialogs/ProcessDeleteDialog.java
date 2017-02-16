/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.dialogs;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;

public class ProcessDeleteDialog extends TrayDialog {
    private WorkflowProcess process;

    public WorkflowProcess getProcess() {
        return process;
    }

    private boolean includeInstances;

    public boolean isIncludeInstances() {
        return includeInstances;
    }

    private Button includeInstancesCheckbox;

    public ProcessDeleteDialog(Shell shell, WorkflowProcess process) {
        super(shell);
        this.process = process;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.getShell().setText("Delete Process");

        String version = process.hasDescendantProcessVersions() ? "(all versions)"
                : "v" + process.getVersionString();
        new Label(composite, SWT.NONE).setText("'" + process.getName() + "' " + version);

        includeInstancesCheckbox = new Button(composite, SWT.CHECK | SWT.LEFT);
        includeInstancesCheckbox.setText("Include process instances if they exist");

        return composite;
    }

    @Override
    protected void okPressed() {
        includeInstances = includeInstancesCheckbox.getSelection();
        super.okPressed();
    }
}
