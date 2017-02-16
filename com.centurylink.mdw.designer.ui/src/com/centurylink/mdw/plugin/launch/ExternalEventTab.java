/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * TODO: Ability to add message headers.
 */
public class ExternalEventTab extends AbstractLaunchConfigurationTab {
    private WorkflowProject workflowProject;

    private Button externalEventCheckbox;
    private Text requestText;

    private Image image = MdwPlugin.getImageDescriptor("icons/extevent.gif").createImage();

    public enum Mode {
        ProcessLaunch, ExternalEvent
    }

    private Mode mode;

    public Mode getMode() {
        return mode;
    }

    public ExternalEventTab(Mode mode) {
        this.mode = mode;
    }

    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        setControl(composite);

        GridLayout topLayout = new GridLayout();
        topLayout.numColumns = 2;
        composite.setLayout(topLayout);

        createExternalEventSection(composite);
    }

    public String getName() {
        return "External Event";
    }

    public Image getImage() {
        return image;
    }

    public void initializeFrom(ILaunchConfiguration launchConfig) {
        try {
            String wfProject = launchConfig
                    .getAttribute(WorkflowLaunchConfiguration.WORKFLOW_PROJECT, "");
            workflowProject = WorkflowProjectManager.getInstance().getWorkflowProject(wfProject);

            if (mode.equals(Mode.ProcessLaunch)) {
                boolean launchViaExternalEvent = launchConfig
                        .getAttribute(ProcessLaunchConfiguration.LAUNCH_VIA_EXTERNAL_EVENT, false);
                externalEventCheckbox.setSelection(launchViaExternalEvent);
                requestText.setEnabled(launchViaExternalEvent);
            }

            String request = launchConfig
                    .getAttribute(WorkflowLaunchConfiguration.EXTERNAL_EVENT_REQUEST, "");
            requestText.setText(request);

            validatePage();
        }
        catch (CoreException ex) {
            PluginMessages.uiError(ex, "Launch Init", workflowProject);
        }
    }

    public void performApply(ILaunchConfigurationWorkingCopy launchConfig) {
        if (mode.equals(Mode.ProcessLaunch)) {
            boolean launchViaExternalEvent = externalEventCheckbox.getSelection();
            launchConfig.setAttribute(ProcessLaunchConfiguration.LAUNCH_VIA_EXTERNAL_EVENT,
                    launchViaExternalEvent);
            if (launchViaExternalEvent)
                launchConfig.setAttribute(ProcessLaunchConfiguration.NOTIFY_PROCESS, false);
        }

        String request = requestText.getText();
        launchConfig.setAttribute(WorkflowLaunchConfiguration.EXTERNAL_EVENT_REQUEST, request);
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy launchConfig) {
        if (mode.equals(Mode.ProcessLaunch))
            launchConfig.setAttribute(ProcessLaunchConfiguration.LAUNCH_VIA_EXTERNAL_EVENT, false);

        launchConfig.setAttribute(WorkflowLaunchConfiguration.EXTERNAL_EVENT_REQUEST, "");
    }

    private void createExternalEventSection(Composite parent) {
        if (mode.equals(Mode.ProcessLaunch)) {
            externalEventCheckbox = new Button(parent, SWT.CHECK);
            externalEventCheckbox.setText("Launch via External Event");
            externalEventCheckbox.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    requestText.setEnabled(externalEventCheckbox.getSelection());
                    setDirty(true);
                    validatePage();
                }
            });
        }

        Group requestGroup = new Group(parent, SWT.NONE);
        requestGroup.setText("External Event Request");
        GridLayout gl = new GridLayout();
        gl.numColumns = 1;
        requestGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        requestGroup.setLayoutData(gd);

        requestText = new Text(requestGroup, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        gd = new GridData(GridData.FILL_BOTH);
        requestText.setLayoutData(gd);
        requestText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                setDirty(true);
                validatePage();
            }
        });
    }

    private void validatePage() {
        setErrorMessage(null);
        setMessage(null);

        if (mode.equals(Mode.ExternalEvent)
                || (mode.equals(Mode.ProcessLaunch) && externalEventCheckbox.getSelection())) {
            if (requestText.getText().trim().length() == 0) {
                setErrorMessage("Please enter the event request data.");
                updateLaunchConfigurationDialog();
                return;
            }
        }

        updateLaunchConfigurationDialog();
    }

    @Override
    public boolean canSave() {
        return getErrorMessage() == null;
    }

    @Override
    public boolean isValid(ILaunchConfiguration launchConfig) {
        return canSave();
    }

    public void dispose() {
        super.dispose();
        image.dispose();
    }
}
