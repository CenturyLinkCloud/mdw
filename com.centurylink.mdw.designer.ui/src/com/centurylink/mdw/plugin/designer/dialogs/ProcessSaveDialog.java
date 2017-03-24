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
package com.centurylink.mdw.plugin.designer.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.dataaccess.DataAccessOfflineException;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.model.Versionable.Increment;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

public class ProcessSaveDialog extends TrayDialog {
    public static final int CANCEL = 1;
    public static final int DONT_SAVE = 2;
    public static final int OVERWRITE = 3;
    public static final int NEW_VERSION = 4;
    public static final int FORCE_UPDATE = 5;

    private static final String INSTANCES_MESSAGE = "There are instances associated with this process version.";

    private WorkflowProcess process;

    public WorkflowProcess getProcess() {
        return process;
    }

    private int version;

    public int getVersion() {
        return version;
    }

    private boolean respondingToClose;

    private Button overwriteButton;
    private Button newMinorButton;
    private Button newMajorButton;

    private Button rememberSelectionCheckbox;
    private Button carryOverrideAttributesCheckbox;
    private Button keepLockedCheckbox;
    private Button enforceValidationCheckbox;

    private boolean allowForceUpdate = true;

    public void setAllowForceUpdate(boolean allow) {
        this.allowForceUpdate = allow;
    }

    public ProcessSaveDialog(Shell shell, WorkflowProcess processVersion,
            boolean respondingToClose) {
        super(shell);
        this.process = processVersion;
        this.respondingToClose = respondingToClose;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.getShell().setText("Save Process");

        String msg = "'" + process.getName() + "'";
        if (respondingToClose)
            msg += " has been modified.  Save changes?";
        new Label(composite, SWT.NONE).setText(msg);

        String oldEmbedded = getProcess().getAttribute("process_old_embedded"); // MDW3

        Group radioGroup = new Group(composite, SWT.NONE);
        radioGroup.setText("Process Version to Save");
        GridLayout gl = new GridLayout();
        radioGroup.setLayout(gl);
        GridData gd = new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        radioGroup.setLayoutData(gd);
        overwriteButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
        if (oldEmbedded != null || !allowForceUpdate)
            overwriteButton.setEnabled(false);
        overwriteButton.setText("Overwrite current version: " + process.getVersionString());
        overwriteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                versionButtonSelected(e);
            }
        });
        newMinorButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
        newMinorButton.setText("Save as new minor version: " + process.getNewVersionString(false));
        newMinorButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                versionButtonSelected(e);
            }
        });
        newMajorButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
        newMajorButton.setText("Save as new major version: " + process.getNewVersionString(true));
        newMajorButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                versionButtonSelected(e);
            }
        });

        if (oldEmbedded != null)
            new Label(composite, SWT.NONE).setText(
                    "This process contains MDW 3.* subprocesses.\nYou must save it as a new version.");

        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();

        String prefsIncrement = prefsStore
                .getString(PreferenceConstants.PREFS_PROCESS_SAVE_INCREMENT);
        boolean rememberSelection = !prefsIncrement.isEmpty();
        if (prefsIncrement.isEmpty())
            prefsIncrement = Increment.Minor.toString();
        Increment increment = Increment.valueOf(prefsIncrement);
        if (increment == Increment.Overwrite)
            overwriteButton.setSelection(true);
        else if (increment == Increment.Major)
            newMajorButton.setSelection(true);
        else
            newMinorButton.setSelection(true);

        rememberSelectionCheckbox = new Button(composite, SWT.CHECK);
        rememberSelectionCheckbox.setText("Remember this selection for future saves");
        rememberSelectionCheckbox.setSelection(rememberSelection);

        enforceValidationCheckbox = new Button(composite, SWT.CHECK);
        enforceValidationCheckbox.setText("Enforce process validation rules");
        enforceValidationCheckbox.setSelection(
                prefsStore.getBoolean(PreferenceConstants.PREFS_ENFORCE_PROCESS_VALIDATION_RULES));

        if (process.getProject().isFilePersist()) {
            boolean overwrite = overwriteButton.getSelection();
            // override attributes
            carryOverrideAttributesCheckbox = new Button(composite, SWT.CHECK);
            carryOverrideAttributesCheckbox.setText("Carry forward override attributes");
            boolean enabled = process.overrideAttributesApplied() && !overwrite;
            boolean checked = overwrite ? true : process.overrideAttributesApplied();
            enableCarryOverrideAttrs(enabled, checked);
            carryOverrideAttributesCheckbox.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    if (carryOverrideAttributesCheckbox.getSelection()) {
                        WarningTray tray = getWarningTray();
                        if (tray.getMessage().startsWith("Any override attributes")) {
                            tray.close();
                            getButton(Dialog.OK).setText(respondingToClose ? "Save" : "OK");
                        }
                    }
                }
            });
        }
        else {
            keepLockedCheckbox = new Button(composite, SWT.CHECK);
            keepLockedCheckbox.setText("Keep process locked after saving");
            keepLockedCheckbox.setSelection(prefsStore
                    .getBoolean(PreferenceConstants.PREFS_KEEP_PROCESSES_LOCKED_WHEN_SAVING));
        }

        return composite;
    }

    private WarningTray warningTray;

    public WarningTray getWarningTray() {
        if (warningTray == null)
            warningTray = new WarningTray(this);
        return warningTray;
    }

    private void versionButtonSelected(SelectionEvent e) {
        if (!((Button) e.getSource()).getSelection())
            return;

        if (overwriteButton.getSelection()) {
            WarningTray tray = getWarningTray();
            if (tray.getMessage().startsWith("Any override attributes"))
                getButton(Dialog.OK).setText(respondingToClose ? "Save" : "OK");
            tray.close();
            enableCarryOverrideAttrs(false, true);
        }
        else {
            WarningTray tray = getWarningTray();
            if (tray.getMessage().startsWith(INSTANCES_MESSAGE)) {
                tray.close();
                getButton(Dialog.OK).setText(respondingToClose ? "Save" : "OK");
            }
            enableCarryOverrideAttrs(process.overrideAttributesApplied(),
                    process.overrideAttributesApplied());
        }
    }

    @Override
    protected void cancelPressed() {
        setReturnCode(CANCEL);
        close();
    }

    @Override
    protected void okPressed() {
        String warning = null;

        if (overwriteButton.getSelection() && !getButton(Dialog.OK).getText().equals("Force Update")
                && !getButton(Dialog.OK).getText().equals("Force Save") && hasInstances()) {
            warning = INSTANCES_MESSAGE + "\n\n"
                    + "It is recommended that you increment the version when saving\n"
                    + "rather than overwriting the existing version.  Otherwise Designer\n"
                    + "may not correctly display previous incompatible instances.\n\n"
                    + "To update the existing version, click 'Force Update' to confirm.";

            getButton(Dialog.OK).setText("Force Update");
        }
        else if (!getButton(Dialog.OK).getText().equals("Force Save")
                && process.getProject().isProduction()) {
            warning = "This process is for project '"
                    + getProcess().getProject().getSourceProjectName()
                    + "',\nwhich is flagged as a production environment.\n\nPlease click 'Force Save' to confirm.\n\nThis action will be audit logged.";

            getButton(Dialog.OK).setText("Force Save");
        }
        else if (MdwPlugin.getSettings().isWarnOverrideAttrsNotCarriedForward()
                && process.getProject().isFilePersist() && !overwriteButton.getSelection()
                && !carryOverrideAttributesCheckbox.getSelection()
                && !getButton(Dialog.OK).getText().equals("Save Anyway")) {
            warning = "Any override attributes will not be carried forward";
            if (!process.overrideAttributesApplied())
                warning += "\n(server not running when process loaded).";
            else
                warning += ".";
            getButton(Dialog.OK).setText("Save Anyway");
        }

        if (warning != null) {
            WarningTray tray = getWarningTray();
            tray.setMessage(warning);
            tray.open();
            return;
        }

        if (overwriteButton.getSelection()) {
            if (getButton(Dialog.OK).getText().equals("Force Update"))
                setReturnCode(FORCE_UPDATE);
            else
                setReturnCode(OVERWRITE);
        }
        else {
            if (newMinorButton.getSelection())
                version = process.getNewVersion(false);
            else if (newMajorButton.getSelection())
                version = process.getNewVersion(true);

            setReturnCode(NEW_VERSION);
        }

        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
        if (rememberSelectionCheckbox.getSelection()) {
            Increment increment = Increment.Minor;
            if (newMajorButton.getSelection())
                increment = Increment.Major;
            else if (overwriteButton.getSelection())
                increment = Increment.Overwrite;
            prefsStore.setValue(PreferenceConstants.PREFS_PROCESS_SAVE_INCREMENT,
                    increment.toString());
        }
        else {
            prefsStore.setValue(PreferenceConstants.PREFS_PROCESS_SAVE_INCREMENT, "");
        }
        prefsStore.setValue(PreferenceConstants.PREFS_ENFORCE_PROCESS_VALIDATION_RULES,
                enforceValidationCheckbox.getSelection());
        if (!process.getProject().isFilePersist())
            prefsStore.setValue(PreferenceConstants.PREFS_KEEP_PROCESSES_LOCKED_WHEN_SAVING,
                    keepLockedCheckbox.getSelection());
        if (carryOverrideAttributesCheckbox != null)
            prefsStore.setValue(PreferenceConstants.PREFS_CARRY_FORWARD_OVERRIDE_ATTRS,
                    carryOverrideAttributesCheckbox.getSelection());

        close();
    }

    protected void dontSavePressed() {
        setReturnCode(DONT_SAVE);
        close();
    }

    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, respondingToClose ? "Save" : "OK", true);
        if (respondingToClose) {
            Button dontSaveButton = createButton(parent, IDialogConstants.CLOSE_ID, "Don't Save",
                    false);
            dontSaveButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    dontSavePressed();
                }
            });
        }
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    private boolean hasInstances;

    private boolean hasInstances() {
        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
            public void run() {
                DesignerProxy designerProxy = process.getProject().getDesignerProxy();
                try {
                    hasInstances = designerProxy.hasInstances(process);
                }
                catch (DataAccessOfflineException ex) {
                    if (MdwPlugin.getSettings().isLogConnectErrors())
                        PluginMessages.log(ex);
                }
                catch (Exception ex) {
                    throw new RuntimeException(ex.getMessage(), ex);
                }
            }
        });
        return hasInstances;
    }

    private void enableCarryOverrideAttrs(boolean enabled, boolean checked) {
        if (carryOverrideAttributesCheckbox != null) {
            carryOverrideAttributesCheckbox.setSelection(checked);
            carryOverrideAttributesCheckbox.setEnabled(enabled);
        }
    }
}
