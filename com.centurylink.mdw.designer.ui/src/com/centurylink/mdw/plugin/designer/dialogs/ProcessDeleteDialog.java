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
