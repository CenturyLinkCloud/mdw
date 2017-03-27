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
package com.centurylink.mdw.plugin.codegen.event;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.codegen.CodeGenWizardPage;
import com.centurylink.mdw.plugin.codegen.event.EventHandlerWizard.HandlerAction;
import com.centurylink.mdw.plugin.codegen.meta.EventHandler;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.swt.widgets.CTreeCombo;
import com.centurylink.swt.widgets.CTreeComboItem;

public class EventHandlerPage extends CodeGenWizardPage {
    private Button builtInHandlerButton;
    private Button customHandlerButton;

    private Text messagePatternTextField;
    private Text eventNameTextField;

    private Group launchNotifyGroup;
    private Button launchProcessButton;
    private CTreeCombo processTreeCombo;
    private Button launchSynchronousCheckbox;
    private Button notifyProcessButton;
    private Text eventTextField;

    private Button createDocumentCheckbox;
    private Text documentVariableTextField;

    private boolean is6;

    public EventHandlerPage() {
        setTitle("External Event Handler Settings");
        setDescription("Enter the information for your Event Handler.\n"
                + "This will be used to create the external event in MDW.");
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

        is6=getProject().checkRequiredVersion(6, 0);

        createWorkflowProjectControls(composite, ncol);
        createWorkflowPackageControls(composite, ncol);
        if (is6)
            createEventNameControls(composite, ncol);
        createMessagePatternControls(composite, ncol);
        createXPathHelpControls(composite, ncol);
        createEventHandlerControls(composite, ncol);
        createExternalEventHelpControls(composite, ncol, true);
        setControl(composite);
    }

    protected void createMessagePatternControls(Composite parent, int ncol) {
        Label label = new Label(parent, SWT.NONE);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.verticalIndent = 10;
        label.setLayoutData(gd);
        label.setText("Message Pattern:");
        messagePatternTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol - 1;
        gd.verticalIndent = 10;
        gd.widthHint = 300;
        messagePatternTextField.setLayoutData(gd);
        messagePatternTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                getEventHandler().setMessagePattern(messagePatternTextField.getText().trim());
                handleFieldChanged();
            }
        });
    }

    protected void createEventNameControls(Composite parent, int ncol) {
        Label label = new Label(parent, SWT.NONE);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.verticalIndent = 10;
        label.setLayoutData(gd);
        label.setText("Event Handler Name:");
        eventNameTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol - 1;
        gd.verticalIndent = 10;
        gd.widthHint = 300;
        eventNameTextField.setLayoutData(gd);
        eventNameTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                getEventHandler().setEventName(eventNameTextField.getText().trim());
                handleFieldChanged();
            }
        });
    }

    protected void createXPathHelpControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("");
        Link link = new Link(parent, SWT.SINGLE);
        GridData gd = new GridData(GridData.BEGINNING | GridData.FILL_HORIZONTAL);
        gd.widthHint = getMaxFieldWidth();
        gd.horizontalSpan = ncol - 1;
        link.setText(" <A>MDW XPath Syntax Help</A>");
        link.setLayoutData(gd);
        link.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String href = "/" + MdwPlugin.getPluginId() + "/help/doc/xpath.html";
                PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(href);
            }
        });
    }

    protected void createEventHandlerControls(Composite parent, int ncol) {
        Group radioGroup = new Group(parent, SWT.NONE);
        radioGroup.setText("Handler Type");
        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        radioGroup.setLayout(gl);
        GridData gd = new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = ncol;
        radioGroup.setLayoutData(gd);
        builtInHandlerButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 2;
        builtInHandlerButton.setLayoutData(gd);
        builtInHandlerButton.setSelection(true);
        builtInHandlerButton.setText("Built-in Handler");
        builtInHandlerButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                getEventHandler().setCustom(!builtInHandlerButton.getSelection());
                enableHandlerTypeFields(getEventHandler().isCustom());
                handleFieldChanged();
            }
        });

        createProcessLaunchNotifyControls(radioGroup, false, ncol);
        customHandlerButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 2;
        customHandlerButton.setLayoutData(gd);
        customHandlerButton.setText("Custom Handler");
        customHandlerButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                getEventHandler().setCustom(customHandlerButton.getSelection());
                enableHandlerTypeFields(getEventHandler().isCustom());
                handleFieldChanged();
            }
        });

        createCodeGenerationControls(radioGroup, false, 3);

        enableHandlerTypeFields(getEventHandler().isCustom());
    }

    protected void createProcessLaunchNotifyControls(Composite parent, boolean serviceOption,
            int ncol) {
        launchNotifyGroup = new Group(parent, SWT.NONE);
        launchNotifyGroup.setText("Handler Action");
        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        launchNotifyGroup.setLayout(gl);
        GridData gd = new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = ncol;
        if (!serviceOption)
            gd.horizontalIndent = 25;
        launchNotifyGroup.setLayoutData(gd);

        launchProcessButton = new Button(launchNotifyGroup, SWT.RADIO | SWT.LEFT);
        launchProcessButton.setText("Process Launch:");
        launchProcessButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (launchProcessButton.getSelection())
                    getEventHandlerWizard().setHandlerAction(HandlerAction.launchProcess);
                else
                    getEventHandlerWizard().setHandlerAction(HandlerAction.notifyProcess);
                enableHandlerActionFields(getEventHandlerWizard().getHandlerAction());
            }
        });
        launchProcessButton.setSelection(true);

        // process treecombo
        processTreeCombo = new CTreeCombo(launchNotifyGroup, SWT.BORDER | SWT.FULL_SELECTION);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 250;
        gd.heightHint = 16;
        processTreeCombo.setLayoutData(gd);
        fillProcessTreeCombo();
        processTreeCombo.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                CTreeComboItem[] selItems = processTreeCombo.getSelection();
                if (selItems.length == 1) {
                    CTreeComboItem selItem = selItems[0];
                    if (selItem.getParentItem() == null) {
                        // ignore package selection
                        processTreeCombo.setSelection(new CTreeComboItem[0]);
                    }
                    else {
                        try {
                            Thread.sleep(200);
                        }
                        catch (InterruptedException ex) {
                        }

                        String processPath = selItem.getText();
                        if (getProject().checkRequiredVersion(5, 2))
                            processPath = selItem.getParentItem().getText() + "/" + processPath;
                        getEventHandler().setProcess(processPath);

                        processTreeCombo.dropDown(false);
                        handleFieldChanged();
                    }
                }
            }
        });
        processTreeCombo.addListener(SWT.Modify, new Listener() {
            public void handleEvent(Event event) {
                if (processTreeCombo.getSelection().length == 0) {
                    // triggered when something was typed in the combo instead
                    // of selecting -- use it verbatim
                    // note: also triggered on selection, but immediately
                    // followed by SWT.Selection event, so no harm done
                    getEventHandler().setProcess(processTreeCombo.getText().trim());
                    handleFieldChanged();
                }
            }
        });

        if (serviceOption) {
            // synchronous checkbox
            launchSynchronousCheckbox = new Button(launchNotifyGroup, SWT.CHECK);
            gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
            gd.horizontalSpan = 2;
            gd.horizontalIndent = 25;
            launchSynchronousCheckbox.setLayoutData(gd);
            launchSynchronousCheckbox.setText("Invoke synchronously (service process)");
            launchSynchronousCheckbox.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    boolean launchSync = launchSynchronousCheckbox.getSelection();
                    getEventHandler().setLaunchSynchronous(launchSync);
                    handleFieldChanged();
                }
            });
        }

        notifyProcessButton = new Button(launchNotifyGroup, SWT.RADIO | SWT.LEFT);
        notifyProcessButton.setText("Event Notify:");
        notifyProcessButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (notifyProcessButton.getSelection())
                    getEventHandlerWizard().setHandlerAction(HandlerAction.notifyProcess);
                else
                    getEventHandlerWizard().setHandlerAction(HandlerAction.launchProcess);
                enableHandlerActionFields(getEventHandlerWizard().getHandlerAction());
            }
        });

        // event text
        eventTextField = new Text(launchNotifyGroup, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 250;
        eventTextField.setLayoutData(gd);
        eventTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                getEventHandler().setEvent(eventTextField.getText().trim());
                handleFieldChanged();
            }
        });

        enableHandlerActionFields(getEventHandlerWizard().getHandlerAction());
    }

    private void fillProcessTreeCombo() {
        processTreeCombo.removeAll();
        List<WorkflowPackage> packages = getProject().getTopLevelUserVisiblePackages();
        for (WorkflowPackage pkg : packages) {
            CTreeComboItem packageItem = new CTreeComboItem(processTreeCombo, SWT.NONE);
            packageItem.setText(pkg.getName());
            packageItem.setImage(pkg.getIconImage());
            for (WorkflowProcess process : pkg.getProcesses()) {
                CTreeComboItem processItem = new CTreeComboItem(packageItem, SWT.NONE);
                processItem.setText(process.getName());
                processItem.setImage(process.getIconImage());
            }
        }
    }

    private void enableHandlerTypeFields(boolean isCustom) {
        if (isCustom)
            customHandlerButton.setSelection(true);
        else
            builtInHandlerButton.setSelection(true);
        enableCodeGenerationControls(isCustom);
        enableLaunchNotifyGroup(!isCustom);
    }

    private void enableLaunchNotifyGroup(boolean enabled) {
        launchNotifyGroup.setEnabled(enabled);
        launchProcessButton.setEnabled(enabled);
        notifyProcessButton.setEnabled(enabled);

        if (enabled) {
            enableHandlerActionFields(getEventHandlerWizard().getHandlerAction());
        }
        else {
            processTreeCombo.setEnabled(false);
            processTreeCombo.setEditable(false);
            if (launchSynchronousCheckbox != null)
                launchSynchronousCheckbox.setEnabled(false);
            eventTextField.setEnabled(false);
        }
    }

    private void enableHandlerActionFields(HandlerAction handlerAction) {
        processTreeCombo.setEnabled(handlerAction == HandlerAction.launchProcess);
        processTreeCombo.setEditable(handlerAction == HandlerAction.launchProcess);
        if (launchSynchronousCheckbox != null)
            launchSynchronousCheckbox.setEnabled(handlerAction == HandlerAction.launchProcess);
        eventTextField.setEnabled(handlerAction == HandlerAction.notifyProcess);
    }

    protected void createDocumentControls(Composite parent, int ncol) {
        createDocumentCheckbox = new Button(parent, SWT.CHECK);
        createDocumentCheckbox.setText("Create a Document:");
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalIndent = 5;
        gd.verticalIndent = 3;

        createDocumentCheckbox.setLayoutData(gd);
        createDocumentCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean createDocument = createDocumentCheckbox.getSelection();
                getEventHandler().setCreateDocument(createDocument);
                if (!createDocument)
                    documentVariableTextField.setText("");
                documentVariableTextField.setEnabled(createDocument);
                handleFieldChanged();
            }
        });

        documentVariableTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 200;
        gd.verticalIndent = 5;
        gd.horizontalSpan = ncol - 1;
        documentVariableTextField.setLayoutData(gd);
        documentVariableTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                getEventHandler().setDocumentVariable(documentVariableTextField.getText().trim());
                handleFieldChanged();
            }
        });

        enableDocumentField(false);
    }

    protected void enableDocumentField(boolean enabled) {
        documentVariableTextField.setEnabled(false);
    }

    protected void createExternalEventHelpControls(Composite parent, int ncol, boolean indent) {
        if (indent)
            new Label(parent, SWT.NONE).setText("");
        Link link = new Link(parent, SWT.SINGLE);
        GridData gd = new GridData(GridData.BEGINNING | GridData.FILL_HORIZONTAL);
        gd.widthHint = getMaxFieldWidth();
        gd.horizontalSpan = ncol - 1;
        if (!indent)
            gd.verticalIndent = 3;
        link.setText(" <A>External Event Handler Help</A>");
        link.setLayoutData(gd);
        link.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String href = "/" + MdwPlugin.getPluginId() + "/help/doc/listener.html";
                PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(href);
            }
        });
    }

    public EventHandler getEventHandler() {
        return (EventHandler) getCodeElement();
    }

    protected EventHandlerWizard getEventHandlerWizard() {
        return (EventHandlerWizard) getWizard();
    }

    @Override
    protected boolean isPageValid() {
        if (getEventHandler().getProject() == null)
            return false;

        return getStatuses() == null;
    }

    public IStatus[] getStatuses() {
        String msg = null;
        if (getEventHandler().getPackage() == null)
            msg = "Please select a valid workflow package";
        else if (!getEventHandler().getPackage().isUserAuthorized(UserRoleVO.ASSET_DESIGN))
            msg = "You're not authorized to create external event handlers for this workflow package.";
        else if (is6 && !checkString(getEventHandler().getEventName()))
            msg = "Please enter a valid event name";
        else if (is6 && getEventHandler().getProject()
                .externalEventNameExists(getEventHandler().getEventName()))
            msg = "An event handler with event name '" + getEventHandler().getEventName()
                    + "' already exists";
        else if (!checkString(getEventHandler().getMessagePattern()))
            msg = "Please enter a valid message pattern";
        else if (getEventHandler().getProject()
                .externalEventMessagePatternExists(getEventHandler().getMessagePattern()))
            msg = "An event handler with message pattern '" + getEventHandler().getMessagePattern()
                    + "' already exists";

        if (msg == null && !getEventHandler().isCustom()) {
            if (getEventHandlerWizard().getHandlerAction() == null)
                msg = "ERROR: Missing built-in handler action"; // should never
                                                                // happen
            else if (getEventHandlerWizard().getHandlerAction() == HandlerAction.launchProcess
                    && !checkString(getEventHandler().getProcess()))
                msg = "Please select a process to invoke via the built-in handler";
            else if (getEventHandlerWizard().getHandlerAction() == HandlerAction.notifyProcess
                    && !checkString(getEventHandler().getEvent()))
                msg = "Please enter an Event ID for the built-in handler notification";
        }

        if (msg == null)
            return null;
        else
            return new IStatus[] { new Status(IStatus.ERROR, getPluginId(), 0, msg, null) };
    }

    @Override
    public IWizardPage getNextPage() {
        if (getWizard() != null) {
            if (getEventHandler().isCustom()) {
                CodeGenWizardPage javaImplPage = selectJavaImplCodeGenPage();
                if (javaImplPage != null)
                    return javaImplPage;
            }
            else {
                return null;
            }
        }

        return super.getNextPage();
    }

    public WorkflowProject getProject() {
        return getEventHandler().getProject();
    }
}
