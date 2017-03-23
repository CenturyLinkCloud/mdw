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

import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

import com.centurylink.mdw.plugin.PluginMessages;

public class ServiceMixMdwServerSection extends ServerEditorSection {
    private Text serverPortTextField;
    private Text sshPortTextField;
    private Text userTextField;
    private Text passwordTextField;

    @Override
    public void createSection(Composite parent) {
        super.createSection(parent);

        FormToolkit toolkit = new FormToolkit(getShell().getDisplay());

        Section section = toolkit.createSection(parent,
                ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION
                        | ExpandableComposite.FOCUS_TITLE | ExpandableComposite.EXPANDED);
        section.setText("MDW Server for " + getServerLabel());
        section.setDescription(server.getAttribute(ServiceMixServer.LOCATION, ""));
        section.setLayoutData(
                new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

        Composite composite = toolkit.createComposite(section);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 5;
        layout.marginWidth = 10;
        layout.verticalSpacing = 5;
        layout.horizontalSpacing = 15;
        composite.setLayout(layout);
        composite.setLayoutData(
                new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
        toolkit.paintBordersFor(composite);
        section.setClient(composite);

        new Label(composite, SWT.NONE).setText("HTTP Port:");
        serverPortTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 125;
        serverPortTextField.setLayoutData(gd);
        int port = server.getAttribute(ServiceMixServer.SERVER_PORT, 0);
        if (port > 0)
            serverPortTextField.setText(Integer.toString(port));
        serverPortTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                try {
                    IUndoableOperation cmd = new ServerAttributeSetterCommand(server,
                            ServiceMixServer.SERVER_PORT,
                            Integer.parseInt(serverPortTextField.getText().trim()), 0);
                    execute(cmd);
                }
                catch (NumberFormatException ex) {
                    PluginMessages.log(ex);
                }
            }
        });

        new Label(composite, SWT.NONE).setText("SSH Port:");
        sshPortTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 125;
        sshPortTextField.setLayoutData(gd);
        int sshPort = server.getAttribute(ServiceMixServer.SSH_PORT, 0);
        if (sshPort > 0)
            sshPortTextField.setText(Integer.toString(sshPort));
        sshPortTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                try {
                    IUndoableOperation cmd = new ServerAttributeSetterCommand(server,
                            ServiceMixServer.SSH_PORT,
                            Integer.parseInt(sshPortTextField.getText().trim()), 0);
                    execute(cmd);
                }
                catch (NumberFormatException ex) {
                    PluginMessages.log(ex);
                }
            }
        });

        new Label(composite, SWT.NONE).setText("Karaf User:");
        userTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 125;
        userTextField.setLayoutData(gd);
        userTextField.setText(server.getAttribute(ServiceMixServer.USER, ""));
        userTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                IUndoableOperation cmd = new ServerAttributeSetterCommand(server,
                        ServiceMixServer.USER, userTextField.getText(), "");
                execute(cmd);
            }
        });

        new Label(composite, SWT.NONE).setText("Password:");
        passwordTextField = new Text(composite, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 125;
        passwordTextField.setLayoutData(gd);
        passwordTextField.setText(server.getAttribute(ServiceMixServer.PASSWORD, ""));
        passwordTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                IUndoableOperation cmd = new ServerAttributeSetterCommand(server,
                        ServiceMixServer.PASSWORD, passwordTextField.getText(), "");
                execute(cmd);
            }
        });
    }

    protected String getServerLabel() {
        return "Apache ServiceMix";
    }

}