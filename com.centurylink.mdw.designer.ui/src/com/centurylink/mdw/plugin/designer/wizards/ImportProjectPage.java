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

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlValidationError;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.designer.DiscoveryException;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.workflow.ConfigManagerProjectsDocument;
import com.centurylink.mdw.workflow.ConfigManagerProjectsDocument.ConfigManagerProjects;
import com.centurylink.mdw.workflow.WorkflowApplication;
import com.centurylink.mdw.workflow.WorkflowEnvironment;

public class ImportProjectPage extends WizardPage {
    private Button importFileRadio;
    private Button discoverRadio;
    private Text filePathText;
    private Button browseImportFileButton;
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

        createFileControls(composite, ncol);
        createSpacer(composite, ncol);
        createDiscoveryControls(composite, ncol);
        setControl(composite);

        enableDiscoveryControls(false);
        filePathText.forceFocus();
    }

    private void createFileControls(Composite parent, int ncol) {
        importFileRadio = new Button(parent, SWT.RADIO | SWT.LEFT);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol;
        importFileRadio.setLayoutData(gd);
        importFileRadio.setSelection(true);
        importFileRadio.setText("Import from File");
        importFileRadio.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean selected = importFileRadio.getSelection();
                discoverRadio.setSelection(!selected);
                enableDiscoveryControls(!selected);
                enableFileControls(selected);
                handleFieldChanged();
            }
        });

        Label label = new Label(parent, SWT.NONE);
        label.setText("Projects File:");
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalIndent = 25;
        label.setLayoutData(gd);

        filePathText = new Text(parent, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 350;
        gd.horizontalSpan = ncol - 2;
        filePathText.setLayoutData(gd);
        filePathText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                handleFieldChanged();
            }
        });

        browseImportFileButton = new Button(parent, SWT.PUSH);
        browseImportFileButton.setText("Browse...");
        browseImportFileButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                FileDialog dlg = new FileDialog(getShell());
                String res = dlg.open();
                if (res != null)
                    filePathText.setText(res);
            }
        });
    }

    private void createDiscoveryControls(Composite parent, int ncol) {
        discoverRadio = new Button(parent, SWT.RADIO | SWT.LEFT);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol;
        discoverRadio.setLayoutData(gd);
        discoverRadio.setSelection(false);
        discoverRadio.setText("Discover");
        discoverRadio.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean selected = discoverRadio.getSelection();
                importFileRadio.setSelection(!selected);
                enableFileControls(!selected);
                enableDiscoveryControls(selected);
                handleFieldChanged();
            }
        });

        Label label = new Label(parent, SWT.NONE);
        label.setText("Application:");
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalIndent = 25;
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

    private void enableFileControls(boolean enabled) {
        if (filePathText.isEnabled() != enabled) {
            filePathText.setEnabled(enabled);
            browseImportFileButton.setEnabled(enabled);
        }
    }

    private void enableDiscoveryControls(boolean enabled) {
        if (applicationCombo.isEnabled() != enabled) {
            applicationCombo.setEnabled(enabled);
            if (enabled) {
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
                            PluginMessages.uiError(getShell(), ex, "Discover Workflow Apps",
                                    getProject());
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
        }
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

        if (importFileRadio.getSelection()) {
            try {
                String xml = FileHelper.getFileContents(filePathText.getText().trim());
                ConfigManagerProjectsDocument doc = ConfigManagerProjectsDocument.Factory.parse(xml,
                        Compatibility.namespaceOptions());
                setWizardErrorMessage(validate(doc));
                if (getWizardErrorMessage() == null) {
                    ConfigManagerProjects cfgMgrProjects = doc.getConfigManagerProjects();
                    for (WorkflowApplication workflowApp : cfgMgrProjects.getWorkflowAppList()) {
                        for (WorkflowEnvironment workflowEnv : workflowApp.getEnvironmentList()) {
                            WorkflowProject project = new WorkflowProject(workflowApp, workflowEnv);
                            projectList.add(project);
                        }
                    }
                }
            }
            catch (XmlException ex) {
                PluginMessages.log("Bad XML File: '" + filePathText.getText().trim() + "'");
                PluginMessages.log(ex);
                MessageDialog.openError(getShell(), "Import Projects", "Invalid XML file content");
            }
            catch (Exception ex) {
                PluginMessages.uiError(getShell(), ex, "Import Projects");
            }
        }
        else {
            String workflowApp = applicationCombo.getText();
            if (discoveredWorkflowApps != null) {
                for (WorkflowApplication discoveredApp : discoveredWorkflowApps) {
                    if (workflowApp.equals(discoveredApp.getName())) {
                        // set the project list
                        for (WorkflowEnvironment workflowEnv : discoveredApp.getEnvironmentList()) {
                            WorkflowProject project = new WorkflowProject(discoveredApp,
                                    workflowEnv);
                            projectList.add(project);
                        }
                        break;
                    }
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
        if (importFileRadio.getSelection()) {
            return filePathText != null && checkFile(filePathText.getText());
        }
        else {
            return applicationCombo.getText().length() > 0;
        }
    }

    public IStatus[] getStatuses() {
        String msg = null;

        if (importFileRadio.getSelection()) {
            if (!checkFile(filePathText.getText().trim()))
                msg = "Please enter a valid file path.";
        }
        else {
            if (discoveredWorkflowApps == null)
                msg = "Unable to discover workflow apps";
        }

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