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
package com.centurylink.mdw.plugin.server;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.centurylink.mdw.plugin.MdwPlugin;

public class ServiceMixRuntimeWizardFragment extends WizardFragment {
    private Text runtimeNameTextField;
    private Text serverHomeTextField;
    private Button browseServerHomeButton;
    private Text javaHomeTextField;
    private Button browseJavaHomeButton;

    private ServiceMixRuntime runtime;
    private Composite composite;

    @Override
    public boolean hasComposite() {
        return true;
    }

    @Override
    public Composite createComposite(Composite parent, final IWizardHandle wizard) {
        composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        composite.setLayout(layout);

        runtime = loadRuntime();
        if (runtime == null)
            return composite;

        wizard.setTitle(runtime.getType() + " Server Runtime");
        wizard.setDescription("Specify your " + runtime.getName()
                + " installation root location (not an instance)");
        wizard.setImageDescriptor(MdwPlugin.getImageDescriptor("icons/server_wiz.png"));

        GridData data = new GridData();
        data.verticalAlignment = GridData.FILL;
        data.horizontalAlignment = GridData.FILL;
        composite.setLayoutData(data);

        new Label(composite, SWT.NONE).setText("Name:");
        runtimeNameTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 150;
        gd.horizontalSpan = 2;
        runtimeNameTextField.setLayoutData(gd);
        runtimeNameTextField.setText(runtime.getName());
        runtimeNameTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                runtime.setName(runtimeNameTextField.getText().trim());
                validate(wizard);
            }
        });

        new Label(composite, SWT.NONE).setText(runtime.getType() + " Home:");
        serverHomeTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 350;
        serverHomeTextField.setLayoutData(gd);
        if (runtime.getLocation() != null)
            serverHomeTextField.setText(runtime.getLocation().toFile().toString());
        serverHomeTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                runtime.setLocation(new Path(serverHomeTextField.getText().trim()));
                validate(wizard);
            }
        });

        browseServerHomeButton = new Button(composite, SWT.PUSH);
        browseServerHomeButton.setText("Browse...");
        browseServerHomeButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dlg = new DirectoryDialog(browseServerHomeButton.getShell());
                dlg.setMessage(
                        "Select the directory where " + runtime.getName() + " is installed.");
                String serverHome = dlg.open();
                if (serverHome != null)
                    serverHomeTextField.setText(serverHome);
            }
        });

        // TODO: use installed JREs (see
        // org.eclipse.jst.server.tomcat.ui.internal.TomcatRuntimeComposite)
        new Label(composite, SWT.NONE).setText("Java Home:");
        javaHomeTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 350;
        javaHomeTextField.setLayoutData(gd);
        if (runtime.getJavaHome() != null)
            javaHomeTextField.setText(runtime.getJavaHome());
        javaHomeTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                runtime.setJavaHome(javaHomeTextField.getText().trim());
                validate(wizard);
            }
        });

        browseJavaHomeButton = new Button(composite, SWT.PUSH);
        browseJavaHomeButton.setText("Browse...");
        browseJavaHomeButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dlg = new DirectoryDialog(browseJavaHomeButton.getShell());
                dlg.setMessage("Select the directory where Java is installed.");
                String jdkHome = dlg.open();
                if (jdkHome != null)
                    javaHomeTextField.setText(jdkHome);
            }
        });

        return composite;
    }

    protected ServiceMixRuntime loadRuntime() {
        IRuntimeWorkingCopy runtimeWC = (IRuntimeWorkingCopy) getTaskModel()
                .getObject(TaskModel.TASK_RUNTIME);
        return (ServiceMixRuntime) runtimeWC.loadAdapter(ServiceMixRuntime.class, null);
    }

    @Override
    public boolean isComplete() {
        IRuntimeWorkingCopy runtime = (IRuntimeWorkingCopy) getTaskModel()
                .getObject(TaskModel.TASK_RUNTIME);

        if (runtime == null)
            return false;

        IStatus status = runtime.validate(null);
        return (status == null || status.getSeverity() != IStatus.ERROR);
    }

    protected boolean validate(IWizardHandle wizard) {
        if (runtime == null) {
            wizard.setMessage("", IMessageProvider.ERROR);
            return false;
        }

        IStatus status = runtime.validate();
        if (status == null || status.isOK())
            wizard.setMessage(null, IMessageProvider.NONE);
        else if (status.getSeverity() == IStatus.WARNING)
            wizard.setMessage(status.getMessage(), IMessageProvider.WARNING);
        else
            wizard.setMessage(status.getMessage(), IMessageProvider.ERROR);
        wizard.update();

        return status == null || status.isOK();
    }
}
