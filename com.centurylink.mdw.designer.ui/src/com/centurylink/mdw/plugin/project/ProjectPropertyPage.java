/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public abstract class ProjectPropertyPage extends PropertyPage {
    private WorkflowProject workflowProject;

    public WorkflowProject getProject() {
        return workflowProject;
    }

    protected void initializeWorkflowProject() {
        IProject project = (IProject) getElement();
        WorkflowProjectManager wfProjectManager = WorkflowProjectManager.getInstance();
        workflowProject = wfProjectManager.getWorkflowProject(project);
    }

    protected Composite createComposite(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL);
        data.grabExcessHorizontalSpace = true;
        composite.setLayoutData(data);
        return composite;
    }

    protected Composite createComposite(Composite parent, int ncol) {
        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.numColumns = ncol;
        composite.setLayout(layout);

        GridData data = new GridData();
        data.verticalAlignment = GridData.FILL;
        data.horizontalAlignment = GridData.FILL;
        composite.setLayoutData(data);

        return composite;
    }

    protected void addSeparator(Composite parent) {
        Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        separator.setLayoutData(gridData);
    }

    protected void addHeading(Composite parent, String label) {
        Label heading = new Label(parent, SWT.NONE);
        heading.setText(label);
        heading.setFont(new Font(heading.getDisplay(), new FontData("Tahoma", 8, SWT.BOLD)));
    }
}
