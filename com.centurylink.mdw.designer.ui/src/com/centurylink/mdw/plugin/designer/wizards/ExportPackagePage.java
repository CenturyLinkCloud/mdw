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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

public class ExportPackagePage extends ImportExportPage {
    private Button exportJsonRadio;
    private Button exportZipRadio;
    private Button exportJsonCheckbox;
    private Button includeTaskTemplsCheckbox;
    private Button inferImplsCheckbox;

    public ExportPackagePage() {
        super("Export MDW Package(s)", "Export JSON or XML file for MDW package(s).");
    }

    protected String getDefaultFileName() {
        return getPackage().getName() + "-" + getPackage().getVersionString() + getFileExtension();
    }

    @Override
    protected void createControls(Composite composite, int ncol) {
        super.createControls(composite, ncol);
        if (getProject().isFilePersist()) {
            if (getProject().checkRequiredVersion(6)) {
                createFormatControls(composite, ncol);
            }
            else {
                createExportJson(composite, ncol);
                createIncludeTaskTemplates(composite, ncol);
            }
        }
        else {
            createInferImplementors(composite, ncol);
        }
    }

    private void createFormatControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE); // spacer

        Group radioGroup = new Group(parent, SWT.NONE);
        radioGroup.setText("Export Format");
        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        radioGroup.setLayout(gl);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol - 1;
        gd.verticalIndent = 5;
        radioGroup.setLayoutData(gd);

        exportJsonRadio = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 1;
        gd.verticalIndent = 5;
        exportJsonRadio.setLayoutData(gd);
        exportJsonRadio.setText("JSON");
        exportJsonRadio.setSelection(true);
        exportJsonRadio.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean selected = exportJsonRadio.getSelection();
                exportZipRadio.setSelection(!selected);
                IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
                prefsStore.setValue(PreferenceConstants.PREFS_EXPORT_JSON_FORMAT, selected);
                prefsStore.setValue(PreferenceConstants.PREFS_SUPPRESS_TASK_TEMPLATES_IN_PKG_EXPORT,
                        false);
                handleFieldChanged();
            }
        });
        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
        boolean exportJson = prefsStore.getBoolean(PreferenceConstants.PREFS_EXPORT_JSON_FORMAT);
        exportJsonRadio.setSelection(exportJson);

        exportZipRadio = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 1;
        gd.verticalIndent = 5;
        gd.horizontalIndent = 25;
        exportZipRadio.setLayoutData(gd);
        exportZipRadio.setText("ZIP");
        exportZipRadio.setSelection(false);
        exportZipRadio.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean selected = exportZipRadio.getSelection();
                exportJsonRadio.setSelection(!selected);
                IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
                prefsStore.setValue(PreferenceConstants.PREFS_EXPORT_ZIP_FORMAT, selected);
                prefsStore.setValue(PreferenceConstants.PREFS_SUPPRESS_TASK_TEMPLATES_IN_PKG_EXPORT,
                        false);
                handleFieldChanged();
            }
        });
        boolean exportZip = prefsStore.getBoolean(PreferenceConstants.PREFS_EXPORT_ZIP_FORMAT);
        exportZipRadio.setSelection(exportZip);
    }

    private void createExportJson(Composite parent, int ncol) {
        exportJsonCheckbox = new Button(parent, SWT.CHECK | SWT.LEFT);
        GridData gd = new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = ncol;
        gd.verticalIndent = 10;
        exportJsonCheckbox.setLayoutData(gd);
        exportJsonCheckbox.setText("Export JSON");
        exportJsonCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean checked = exportJsonCheckbox.getSelection();
                IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
                prefsStore.setValue(PreferenceConstants.PREFS_EXPORT_JSON_FORMAT, checked);
            }
        });
        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
        boolean exportJson = prefsStore.getBoolean(PreferenceConstants.PREFS_EXPORT_JSON_FORMAT);
        exportJsonCheckbox.setSelection(exportJson);
    }

    private void createIncludeTaskTemplates(Composite parent, int ncol) {
        includeTaskTemplsCheckbox = new Button(parent, SWT.CHECK | SWT.LEFT);
        GridData gd = new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = ncol;
        gd.verticalIndent = 10;
        includeTaskTemplsCheckbox.setLayoutData(gd);
        includeTaskTemplsCheckbox.setText("Include Task Templates");
        includeTaskTemplsCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean checked = includeTaskTemplsCheckbox.getSelection();
                IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
                prefsStore.setValue(PreferenceConstants.PREFS_SUPPRESS_TASK_TEMPLATES_IN_PKG_EXPORT,
                        !checked);
            }
        });
        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
        boolean includeTaskTemplates = !prefsStore
                .getBoolean(PreferenceConstants.PREFS_SUPPRESS_TASK_TEMPLATES_IN_PKG_EXPORT);
        includeTaskTemplsCheckbox.setSelection(includeTaskTemplates);
    }

    private void createInferImplementors(Composite parent, int ncol) {
        inferImplsCheckbox = new Button(parent, SWT.CHECK | SWT.LEFT);
        GridData gd = new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = ncol;
        gd.verticalIndent = 10;
        inferImplsCheckbox.setLayoutData(gd);
        inferImplsCheckbox.setText("Infer Referenced Activity Implementors");
        inferImplsCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean checked = inferImplsCheckbox.getSelection();
                IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
                prefsStore.setValue(
                        PreferenceConstants.PREFS_SUPPRESS_INFER_REFERENCED_IMPLS_DURING_EXPORT,
                        !checked);
            }
        });
        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
        boolean inferReferencedImpls = !prefsStore.getBoolean(
                PreferenceConstants.PREFS_SUPPRESS_INFER_REFERENCED_IMPLS_DURING_EXPORT);
        inferImplsCheckbox.setSelection(inferReferencedImpls);
    }

    protected String getFileExtension() {
        String format = ".zip";
        if (!getProject().checkRequiredVersion(6))
            format = ".xml";
        return MdwPlugin.getDefault().getPreferenceStore()
                .getBoolean(PreferenceConstants.PREFS_EXPORT_JSON_FORMAT) ? ".json" : format;
    }
}
