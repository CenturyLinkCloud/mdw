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
package com.centurylink.mdw.plugin.codegen;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.codegen.CodeGenWizard.CodeGenType;
import com.centurylink.mdw.plugin.codegen.meta.Code;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public abstract class CodeGenWizardPage extends WizardPage {
    private Combo workflowProjectCombo;
    private Combo workflowPackageCombo;

    private Group codeGenerationGroup;
    private Button localProjectButton;
    private Button dynamicJavaButton;
    private Button registerOnlyButton;

    private Text workflowProjectLabel;
    private Text workflowPackageLabel;
    private Text infoLabelField;

    @Override
    public WorkflowProject getProject() {
        Code codeElement = getCodeElement();
        if (codeElement == null)
            return null;
        else
            return codeElement.getProject();
    }

    @Override
    public void setProject(WorkflowProject project) {
        getCodeElement().setProject(project);
    }

    public WorkflowPackage getPackage() {
        Code codeElement = getCodeElement();
        if (codeElement == null)
            return null;
        else
            return codeElement.getPackage();
    }

    public void setPackage(WorkflowPackage workflowPackage) {
        getCodeElement().setPackage(workflowPackage);
    }

    public void initializeInfo() {
        if (getProject() != null)
            workflowProjectLabel.setText(getProject().getLabel());
        if (getPackage() != null)
            workflowPackageLabel.setText(getPackage().getName());
        if (getCodeElement() != null && getCodeGenWizard().getInfoLabelValue() != null)
            infoLabelField.setText(getCodeGenWizard().getInfoLabelValue());
    }

    public CodeGenWizard getCodeGenWizard() {
        return (CodeGenWizard) getWizard();
    }

    public Code getCodeElement() {
        return getCodeGenWizard().getCodeElement();
    }

    protected void createWorkflowProjectControls(Composite parent, int ncol) {
        createWorkflowProjectControls(parent, ncol, (SelectionListener) null);
    }

    protected void createWorkflowProjectControls(Composite parent, int ncol,
            SelectionListener selectionListener) {
        new Label(parent, SWT.NONE).setText("Workflow Project:");
        workflowProjectCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol - 1;
        gd.widthHint = 250;
        workflowProjectCombo.setLayoutData(gd);
        workflowProjectCombo.removeAll();
        for (WorkflowProject project : WorkflowProjectManager.getInstance().getWorkflowProjects()) {
            workflowProjectCombo.add(project.getSourceProjectName());
        }
        if (selectionListener != null)
            workflowProjectCombo.addSelectionListener(selectionListener);
        workflowProjectCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                        .getWorkflowProject(workflowProjectCombo.getText());
                getCodeElement().setProject(workflowProject);
                enableCodeGenTypes();
                workflowPackageCombo.removeAll();
                if (getCodeElement().getProject() != null) {
                    for (WorkflowPackage packageVersion : getCodeElement().getProject()
                            .getTopLevelUserVisiblePackages())
                        workflowPackageCombo.add(packageVersion.getName());
                }
            }
        });
        if (getCodeElement().getProject() != null)
            workflowProjectCombo.setText(getCodeElement().getProject().getName());
    }

    @Override
    protected void createWorkflowPackageControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("Workflow Package:");
        workflowPackageCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol - 1;
        gd.widthHint = 250;
        workflowPackageCombo.setLayoutData(gd);
        workflowPackageCombo.removeAll();
        if (getCodeElement().getProject() != null) {
            for (WorkflowPackage packageVersion : getCodeElement().getProject()
                    .getTopLevelUserVisiblePackages())
                workflowPackageCombo.add(packageVersion.getName());
        }
        workflowPackageCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String packageName = workflowPackageCombo.getText().trim();
                getCodeElement().setPackage(getCodeElement().getProject().getPackage(packageName));
                handleFieldChanged();
            }
        });
        if (getCodeElement().getPackage() != null)
            workflowPackageCombo.setText(getCodeElement().getPackage().getName());
    }

    protected void createCodeGenerationControls(Composite parent, boolean separateLabel, int ncol) {
        if (separateLabel) {
            Label label = new Label(parent, SWT.NONE);
            label.setText("Code Generation:");
            GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
            gd.verticalIndent = 8;
            label.setLayoutData(gd);
        }

        codeGenerationGroup = new Group(parent, SWT.NONE);
        if (!separateLabel)
            codeGenerationGroup.setText("Code Generation");
        GridLayout gl = new GridLayout();
        gl.numColumns = 1;
        codeGenerationGroup.setLayout(gl);
        GridData gd = new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = ncol;
        if (!separateLabel)
            gd.horizontalIndent = 25;
        codeGenerationGroup.setLayoutData(gd);

        localProjectButton = new Button(codeGenerationGroup, SWT.RADIO | SWT.LEFT);
        localProjectButton.setText("Static Workspace Code");
        localProjectButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                getCodeGenWizard().setCodeGenType(CodeGenType.staticJavaCode);
                handleFieldChanged();
            }
        });

        dynamicJavaButton = new Button(codeGenerationGroup, SWT.RADIO | SWT.LEFT);
        dynamicJavaButton.setText("Dynamic Java Asset (Requires MDW 5.5)");
        dynamicJavaButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                getCodeGenWizard().setCodeGenType(CodeGenType.dynamicJavaCode);
                handleFieldChanged();
            }
        });

        registerOnlyButton = new Button(codeGenerationGroup, SWT.RADIO | SWT.LEFT);
        registerOnlyButton.setText("Registration Only (No source code generation)");
        registerOnlyButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                getCodeGenWizard().setCodeGenType(CodeGenType.registrationOnly);
                handleFieldChanged();
            }
        });

        localProjectButton.setSelection(true);
        getCodeGenWizard().setCodeGenType(CodeGenType.staticJavaCode);
        enableCodeGenTypes();
    }

    protected void createInfoControls(Composite parent, int ncol, String labelFieldLabel) {
        new Label(parent, SWT.NONE).setText("Workflow Project:");
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol - 1;
        gd.widthHint = 250;
        workflowProjectLabel = new Text(parent, SWT.SINGLE | SWT.BORDER);
        workflowProjectLabel.setLayoutData(gd);
        workflowProjectLabel.setEditable(false);
        workflowProjectLabel.setText(getProject().getLabel());

        new Label(parent, SWT.NONE).setText("Workflow Package:");
        workflowPackageLabel = new Text(parent, SWT.SINGLE | SWT.BORDER);
        workflowPackageLabel.setLayoutData(gd);
        workflowPackageLabel.setEditable(false);
        if (getPackage() != null)
            workflowPackageLabel.setText(getPackage().getLabel());

        if (labelFieldLabel != null) {
            new Label(parent, SWT.NONE).setText(labelFieldLabel);
            infoLabelField = new Text(parent, SWT.SINGLE | SWT.BORDER);
            infoLabelField.setLayoutData(gd);
            infoLabelField.setEditable(false);
            if (getCodeGenWizard().getInfoLabelValue() != null)
                infoLabelField.setText(getCodeGenWizard().getInfoLabelValue());
        }
    }

    protected void enableCodeGenTypes() {
        registerOnlyButton.setEnabled(isRegistrationSupported()); // this is
                                                                  // static

        if (getProject().isLocalJavaSupported()) {
            localProjectButton.setEnabled(true);
        }
        else {
            if (localProjectButton.getSelection()) {
                dynamicJavaButton.setSelection(true);
                getCodeGenWizard().setCodeGenType(CodeGenType.dynamicJavaCode);
            }
            localProjectButton.setSelection(false);
            localProjectButton.setEnabled(false);
        }

        if (getProject().checkRequiredVersion(5, 5)) {
            dynamicJavaButton.setEnabled(true);
        }
        else {
            if (dynamicJavaButton.getSelection()) {
                if (getProject().isLocalJavaSupported()) {
                    localProjectButton.setSelection(true);
                    getCodeGenWizard().setCodeGenType(CodeGenType.staticJavaCode);
                }
                else {
                    registerOnlyButton.setSelection(true);
                    getCodeGenWizard().setCodeGenType(CodeGenType.registrationOnly);
                }
            }
            dynamicJavaButton.setEnabled(false);
        }
    }

    protected boolean isRegistrationSupported() {
        return true;
    }

    protected void enableCodeGenerationControls(boolean enabled) {
        codeGenerationGroup.setEnabled(enabled);
        if (enabled)
            enableCodeGenTypes();
        else {
            localProjectButton.setEnabled(false);
            dynamicJavaButton.setEnabled(false);
            registerOnlyButton.setEnabled(false);
        }
    }

    protected CodeGenWizardPage selectJavaImplCodeGenPage() {
        CodeGenWizardPage page = null;
        if (getCodeGenWizard().getCodeGenType() == CodeGenType.staticJavaCode) {
            page = getCodeGenWizard().staticJavaPage;
            page.determinePackageFragmentRoot();
            page.initializeInfo();
        }
        else if (getCodeGenWizard().getCodeGenType() == CodeGenType.dynamicJavaCode) {
            page = getCodeGenWizard().dynamicJavaPage;
            page.initializeInfo();
        }
        else if (getCodeGenWizard().getCodeGenType() == CodeGenType.registrationOnly) {
            page = getCodeGenWizard().registrationOnlyPage;
            page.initializeInfo();
        }

        return page;
    }

    public boolean isPageComplete() {
        return isPageValid();
    }

    protected boolean isPageValid() {
        return true;
    }
}
