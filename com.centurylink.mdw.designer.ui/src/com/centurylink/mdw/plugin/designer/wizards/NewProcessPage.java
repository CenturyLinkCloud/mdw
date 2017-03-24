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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;

public class NewProcessPage extends WizardPage {
    private Text processNameTextField;
    private Text descriptionTextField;
    private Button isRuleSetCheckbox;

    public NewProcessPage() {
        setTitle("New MDW Process");
        setDescription("Enter the name and optional description for your process.");
    }

    @Override
    public void drawWidgets(Composite parent) {
        // create the composite to hold the widgets
        Composite composite = new Composite(parent, SWT.NULL);

        // create the layout for this wizard page
        GridLayout gl = new GridLayout();
        int ncol = 4;
        gl.numColumns = ncol;
        composite.setLayout(gl);

        createWorkflowProjectControls(composite, ncol);
        createWorkflowPackageControls(composite, ncol);
        createNameControls(composite, ncol);
        createDescriptionControls(composite, ncol);
        createRuleSetCheckboxControls(composite, ncol);
        if (getProject() != null) {
            getProcess().setInRuleSet(false);
            isRuleSetCheckbox.setSelection(false);
            if (getProject().getDataAccess().getSupportedSchemaVersion() >= 5002) {
                getProcess().setInRuleSet(true);
                isRuleSetCheckbox.setSelection(true);
                isRuleSetCheckbox.setEnabled(false); // force ruleset
            }
        }

        setControl(composite);

        processNameTextField.forceFocus();
    }

    private void createNameControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("Process Name:");

        processNameTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 300;
        gd.horizontalSpan = ncol - 1;
        processNameTextField.setLayoutData(gd);
        processNameTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                getProcess().setName(processNameTextField.getText().trim());
                handleFieldChanged();
            }
        });
    }

    private void createDescriptionControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("Description:");

        descriptionTextField = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 300;
        gd.heightHint = 100;
        gd.horizontalSpan = ncol - 1;
        descriptionTextField.setLayoutData(gd);
        descriptionTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String description = descriptionTextField.getText().trim();
                if (description.length() == 0)
                    description = null;
                getProcess().setDescription(description);
            }
        });
    }

    private void createRuleSetCheckboxControls(Composite parent, int ncol) {
        isRuleSetCheckbox = new Button(parent, SWT.CHECK | SWT.LEFT);
        GridData gd = new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = ncol;
        gd.verticalIndent = 10;
        isRuleSetCheckbox.setLayoutData(gd);
        isRuleSetCheckbox.setText("Store as Workflow Asset");
        isRuleSetCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean checked = isRuleSetCheckbox.getSelection();
                getProcess().setInRuleSet(checked);
            }
        });
    }

    @Override
    public boolean isPageComplete() {
        return isPageValid();
    }

    boolean isPageValid() {
        return (getProject() != null && getProcess().getPackage() != null
                && getProcess().isUserAuthorized(UserRoleVO.ASSET_DESIGN)
                && checkStringDisallowChars(getProcess().getName(), "/"))
                && !getProject().getDataAccess().processNameExists(getProcess().getPackage().getPackageVO(),getProcess().getName());
    }

    public IStatus[] getStatuses() {
        String msg = null;
        if (getProject() == null)
            msg = "Please select a valid workflow project";
        else if (!getProcess().isUserAuthorized(UserRoleVO.ASSET_DESIGN))
            msg = "You're not authorized to create processes for this workflow package.";
        else if (!checkString(getProcess().getName()))
            msg = "Please enter a process name";
        else if (!checkStringDisallowChars(getProcess().getName(), "/"))
            msg = "Invalid characters in name (/ not allowed)";
        else if (getProject().getDataAccess().processNameExists(getProcess().getPackage().getPackageVO(),getProcess().getName()))
            msg = "Process name already exists";

        if (msg == null)
            return null;

        IStatus[] is = { new Status(IStatus.ERROR, getPluginId(), 0, msg, null) };
        return is;
    }

    public WorkflowProcess getProcess() {
        return ((NewProcessWizard) getWizard()).getProcess();
    }

    @Override
    public WorkflowElement getElement() {
        return getProcess();
    }
}