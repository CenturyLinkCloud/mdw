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
package com.centurylink.mdw.plugin.designer.wizards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlValidationError;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.designer.DiscoveryException;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.workflow.ConfigManagerProjectsDocument;
import com.centurylink.mdw.workflow.WorkflowApplication;
import com.centurylink.mdw.workflow.WorkflowEnvironment;

public class ImportProjectPage extends WizardPage {
    private Combo applicationCombo;

    List<WorkflowApplication> discoveredWorkflowApps;

    public ImportProjectPage() {
        setTitle("Import MDW Projects");
        setDescription("Import MDW Project(s) into your workspace.");
    }

    @Override
    public void drawWidgets(Composite parent) {
        // create the composite to hold the widgets
        Composite composite = new Composite(parent, SWT.NULL);

        // create the layout for this wizard page
        GridLayout gl = new GridLayout();
        int ncol = 3;
        gl.numColumns = ncol;
        composite.setLayout(gl);

        createSpacer(composite, ncol);
        createDiscoveryControls(composite, ncol);
        setControl(composite);

        enableDiscoveryControls(false);
    }

    private void createDiscoveryControls(Composite parent, int ncol) {
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol;
        Label label = new Label(parent, SWT.NONE);
        label.setText("Discover Application:");
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalIndent = 5;
        label.setLayoutData(gd);

        applicationCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 338;
        applicationCombo.setLayoutData(gd);
        applicationCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                handleFieldChanged();
            }
        });
    }

    private void enableDiscoveryControls(boolean enabled) {
        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
            public void run() {
                applicationCombo.removeAll();
                applicationCombo.add("");
                // populate the combo
                try {
                    discoveredWorkflowApps = WorkflowProjectManager.getInstance()
                            .discoverWorkflowApps();
                }
                catch (DiscoveryException ex) {
                    PluginMessages.uiError(getShell(), ex, "Discover Workflow Apps", getProject());
                }
                if (discoveredWorkflowApps != null) {
                    List<String> appNames = new ArrayList<String>();
                    for (WorkflowApplication workflowApp : discoveredWorkflowApps)
                        appNames.add(workflowApp.getName());
                    Collections.sort(appNames);
                    for (String appName : appNames)
                        applicationCombo.add(appName);
                }
            }
        });
    }

    @Override
    public boolean canFlipToNextPage() {
        return isPageComplete();
    }

    @Override
    public IWizardPage getNextPage() {
        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
            public void run() {
                populateProjectList();
            }
        });
        return ((ImportProjectWizard) getWizard()).getProjectSelectPage();
    }

    public void populateProjectList() {
        List<WorkflowProject> projectList = new ArrayList<WorkflowProject>();

        String workflowApp = applicationCombo.getText();
        if (discoveredWorkflowApps != null) {
            for (WorkflowApplication discoveredApp : discoveredWorkflowApps) {
                if (workflowApp.equals(discoveredApp.getName())) {
                    // set the project list
                    for (WorkflowEnvironment workflowEnv : discoveredApp.getEnvironmentList()) {
                        WorkflowProject project = new WorkflowProject(discoveredApp, workflowEnv);
                        projectList.add(project);
                    }
                    break;
                }
            }
        }

        setProjectList(projectList);
        ((ImportProjectWizard) getWizard()).initializeProjectSelectPage();
    }

    public String validate(ConfigManagerProjectsDocument projectsDoc) {
        String errorMessage = null;
        List<XmlValidationError> errors = new ArrayList<XmlValidationError>();
        boolean valid = projectsDoc.validate(new XmlOptions().setErrorListener(errors));
        if (!valid) {
            errorMessage = "";
            for (int i = 0; i < errors.size(); i++) {
                errorMessage += errors.get(i).toString();
                if (i < errors.size() - 1)
                    errorMessage += '\n';
            }
        }

        return errorMessage;
    }

    @Override
    public boolean isPageComplete() {
        return isPageValid();
    }

    boolean isPageValid() {
        return applicationCombo.getText().length() > 0;
    }

    public IStatus[] getStatuses() {
        String msg = null;

        if (discoveredWorkflowApps == null)
            msg = "Unable to discover workflow apps";

        if (msg == null)
            return null;

        IStatus[] is = { new Status(IStatus.ERROR, getPluginId(), 0, msg, null) };
        return is;
    }

    private void setProjectList(List<WorkflowProject> projects) {
        ((ImportProjectWizard) getWizard()).setProjectList(projects);
    }

    private String getWizardErrorMessage() {
        return ((ImportProjectWizard) getWizard()).getErrorMessage();
    }

    private void setWizardErrorMessage(String errorMessage) {
        ((ImportProjectWizard) getWizard()).setErrorMessage(errorMessage);
    }
}