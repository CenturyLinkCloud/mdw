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

import com.centurylink.mdw.plugin.designer.model.ActivityImpl;

public class ActivityImplDeleteDialog extends TrayDialog {
    private ActivityImpl activityImpl;

    public ActivityImpl getActivityImpl() {
        return activityImpl;
    }

    private boolean includeActivities;

    public boolean isIncludeActivities() {
        return includeActivities;
    }

    private Button includeActivitiesCheckbox;

    public ActivityImplDeleteDialog(Shell shell, ActivityImpl activityImpl) {
        super(shell);
        this.activityImpl = activityImpl;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.getShell().setText("Delete Activity Implementor");

        String confMsg = "Delete Activity Implementor '" + activityImpl.getName() + "'?";
        new Label(composite, SWT.NONE).setText(confMsg);

        if (!activityImpl.getProject().isPureMdw52()) {
            includeActivitiesCheckbox = new Button(composite, SWT.CHECK | SWT.LEFT);
            includeActivitiesCheckbox.setText("Include activities if they exist");
        }

        return composite;
    }

    @Override
    protected void okPressed() {
        if (includeActivitiesCheckbox != null)
            includeActivities = includeActivitiesCheckbox.getSelection();
        super.okPressed();
    }
}
