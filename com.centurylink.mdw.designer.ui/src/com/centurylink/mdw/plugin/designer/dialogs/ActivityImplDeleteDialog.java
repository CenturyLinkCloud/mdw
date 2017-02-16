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
