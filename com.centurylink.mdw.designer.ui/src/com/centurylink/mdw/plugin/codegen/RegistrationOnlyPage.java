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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.java.JavaNaming;
import com.centurylink.mdw.plugin.codegen.CodeGenWizard.CodeGenType;

public class RegistrationOnlyPage extends CodeGenWizardPage {
    private Text packageTextField;
    private Text classNameTextField;

    public RegistrationOnlyPage(String title, String description) {
        setTitle(title);
        setDescription(description);
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

        createInfoControls(composite, ncol, getCodeGenWizard().getInfoLabelLabel());
        createSepLine(composite, ncol);
        createPackageControls(composite, ncol);
        createClassNameControls(composite, ncol);

        setControl(composite);
    }

    protected void createPackageControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("Java Package:");

        packageTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 300;
        gd.horizontalSpan = ncol - 1;
        packageTextField.setLayoutData(gd);
        packageTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                getCodeElement().setJavaPackage(packageTextField.getText().trim());
                handleFieldChanged();
            }
        });
    }

    protected void createClassNameControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("Class Name:");

        classNameTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 250;
        gd.horizontalSpan = ncol - 1;
        classNameTextField.setLayoutData(gd);
        classNameTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                getCodeElement().setClassName(classNameTextField.getText().trim());
                handleFieldChanged();
            }
        });
    }

    @Override
    public boolean isPageComplete() {
        return getCodeGenWizard().getCodeGenType() != CodeGenType.registrationOnly || isPageValid();
    }

    @Override
    protected boolean isPageValid() {
        if (!checkString(getCodeElement().getJavaPackage()))
            return false;

        if (!checkString(getCodeElement().getClassName()))
            return false;

        if (getCodeElement().getClassName().indexOf('.') >= 0)
            return false;

        return getCodeGenWizard().validate() == null;
    }

    public IStatus[] getStatuses() {
        String msg = null;
        if (getProject() != null) // is null before inference
        {
            if (getCodeElement().getClassName().contains("."))
                msg = "Java class name must not be qualified.";
            else if (!getCodeElement().getClassName()
                    .equals(JavaNaming.getValidClassName(getCodeElement().getClassName())))
                msg = "Invalid character(s) in class name";
            else
                msg = getCodeGenWizard().validate();
        }

        if (msg == null)
            return null;
        else
            return new IStatus[] { new Status(IStatus.ERROR, getPluginId(), 0, msg, null) };
    }

    @Override
    public IWizardPage getNextPage() {
        CodeGenWizard codeGenWizard = getCodeGenWizard();
        if (codeGenWizard != null)
            return codeGenWizard.getPageAfterJavaImplCodeGenPage();

        return super.getNextPage();
    }

}
