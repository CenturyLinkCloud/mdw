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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;

public class PackageDeleteDialog extends TrayDialog {
    private WorkflowPackage packageToDelete;

    public PackageDeleteDialog(Shell shell, WorkflowPackage packageToDelete) {
        super(shell);
        this.packageToDelete = packageToDelete;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.getShell().setText("Delete Package");
        new Label(composite, SWT.NONE).setText("Delete package '" + packageToDelete.getName()
                + "' v" + packageToDelete.getVersionString() + "?");
        if (packageToDelete.getProject().isFilePersist())
            new Label(composite, SWT.NONE)
                    .setText("All workflow elements within this package will be deleted.");
        else
            new Label(composite, SWT.NONE).setText(
                    "Workflow elements within this package will revert to the default package and will have to be deleted separately.");
        return composite;
    }
}
