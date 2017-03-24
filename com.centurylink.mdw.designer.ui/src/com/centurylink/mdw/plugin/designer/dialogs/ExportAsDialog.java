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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.designer.display.Node;
import com.centurylink.mdw.designer.pages.ExportHelper;
import com.centurylink.mdw.designer.pages.FlowchartPage;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

public class ExportAsDialog extends TrayDialog {
    public static final String HTML = "HTML";
    public static final String MS_WORD = "MS Word";
    public static final String JPG = "JPG";
    public static final String PNG = "PNG";
    public static final String RTF = "RTF";
    public static final String BPMN2 = "BPMN 2";
    public static final String PDF = "PDF";

    public static final Map<String, String> FORMATS = new HashMap<String, String>();

    static {
        FORMATS.put(HTML, "html");
        FORMATS.put(MS_WORD, "docx");
        FORMATS.put(JPG, "jpg");
        FORMATS.put(PNG, "png");
        FORMATS.put(BPMN2, "bpmn");
        FORMATS.put(RTF, "rtf");
        FORMATS.put(PDF, "pdf");
    }

    private WorkflowProcess processVersion;
    private FlowchartPage flowchartPage;

    private Text filepathText;
    private Button browseButton;

    private Button jpegButton;
    private Button pngButton;
    private Button pdfButton;
    private Button rtfButton;
    private Button htmlButton;
    private Button docxButton;
    private Button bpmnButton;

    private Button docButton;
    private Button attrsButton;
    private Button varsButton;

    private Button sequenceIdButton;
    private Button referenceIdButton;
    private Button logicalIdButton;

    private String filepath;

    public String getFilepath() {
        return filepath;
    }

    private String exportType;

    public String getExportType() {
        return exportType;
    }

    public ExportAsDialog(Shell shell, WorkflowProcess processVersion,
            FlowchartPage flowchartPage) {
        super(shell);
        this.processVersion = processVersion;
        this.flowchartPage = flowchartPage;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.getShell().setText("Export Process");

        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        composite.setLayout(gl);

        Label msgLabel = new Label(composite, SWT.NONE);
        msgLabel.setText("Save process '" + processVersion.getName() + "' as image.");
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 2;
        msgLabel.setLayoutData(gd);

        Group exportFormatGroup = new Group(composite, SWT.NONE);
        exportFormatGroup.setText("Format to Export");
        gl = new GridLayout();
        exportFormatGroup.setLayout(gl);
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        exportFormatGroup.setLayoutData(gd);
        bpmnButton = new Button(exportFormatGroup, SWT.RADIO | SWT.LEFT);
        bpmnButton.setText(BPMN2);
        htmlButton = new Button(exportFormatGroup, SWT.RADIO | SWT.LEFT);
        htmlButton.setText(HTML);
        docxButton = new Button(exportFormatGroup, SWT.RADIO | SWT.LEFT);
        docxButton.setText(MS_WORD);
        jpegButton = new Button(exportFormatGroup, SWT.RADIO | SWT.LEFT);
        jpegButton.setText("JPEG");
        jpegButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean sel = jpegButton.getSelection();

                if (sel) {
                    docButton.setSelection(false);
                    attrsButton.setSelection(false);
                    varsButton.setSelection(false);
                }

                docButton.setEnabled(!sel);
                attrsButton.setEnabled(!sel);
                varsButton.setEnabled(!sel);
            }
        });
        pngButton = new Button(exportFormatGroup, SWT.RADIO | SWT.LEFT);
        pngButton.setText(PNG);
        pngButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean sel = pngButton.getSelection();

                if (sel) {
                    docButton.setSelection(false);
                    attrsButton.setSelection(false);
                    varsButton.setSelection(false);
                }

                docButton.setEnabled(!sel);
                attrsButton.setEnabled(!sel);
                varsButton.setEnabled(!sel);
            }
        });
        rtfButton = new Button(exportFormatGroup, SWT.RADIO | SWT.LEFT);
        rtfButton.setText(RTF);
        pdfButton = new Button(exportFormatGroup, SWT.RADIO | SWT.LEFT);
        pdfButton.setText(PDF);

        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
        String format = prefsStore.getString(PreferenceConstants.PREFS_PROCESS_EXPORT_FORMAT);
        if (FORMATS.get(HTML).equals(format) || format == null)
            htmlButton.setSelection(true);
        else if (FORMATS.get(MS_WORD).equals(format))
            docxButton.setSelection(true);
        else if (FORMATS.get(JPG).equals(format))
            jpegButton.setSelection(true);
        else if (FORMATS.get(PNG).equals(format))
            pngButton.setSelection(true);
        else if (FORMATS.get(BPMN2).equals(format))
            bpmnButton.setSelection(true);
        else if (FORMATS.get(RTF).equals(format))
            rtfButton.setSelection(true);
        else if (FORMATS.get(PDF).equals(format))
            pdfButton.setSelection(true);

        Composite comp2 = new Composite(composite, SWT.NONE);
        gl = new GridLayout();
        gl.marginTop = -5;
        comp2.setLayout(gl);

        // inclusion options
        Group optionsGroup = new Group(comp2, SWT.NONE);
        optionsGroup.setText("Include");
        gl = new GridLayout();
        optionsGroup.setLayout(gl);
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        optionsGroup.setLayoutData(gd);
        docButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        docButton.setText("Documentation");
        attrsButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        attrsButton.setText("Attributes");
        varsButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        varsButton.setText("Variables");

        docButton.setSelection(
                prefsStore.getBoolean(PreferenceConstants.PREFS_PROCESS_EXPORT_DOCUMENTATION));
        attrsButton.setSelection(
                prefsStore.getBoolean(PreferenceConstants.PREFS_PROCESS_EXPORT_ATTRIBUTES));
        varsButton.setSelection(
                prefsStore.getBoolean(PreferenceConstants.PREFS_PROCESS_EXPORT_VARIABLES));

        // activity order
        Group activityOrderGroup = new Group(comp2, SWT.NONE);
        activityOrderGroup.setText("Element Order");
        activityOrderGroup.setLayout(new GridLayout());
        activityOrderGroup.setLayoutData(
                new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL));
        sequenceIdButton = new Button(activityOrderGroup, SWT.RADIO | SWT.LEFT);
        sequenceIdButton.setText("Sequence Number");
        referenceIdButton = new Button(activityOrderGroup, SWT.RADIO | SWT.LEFT);
        referenceIdButton.setText("Reference ID");
        logicalIdButton = new Button(activityOrderGroup, SWT.RADIO | SWT.LEFT);
        logicalIdButton.setText("Logical ID");

        String sort = prefsStore.getString(PreferenceConstants.PREFS_PROCESS_EXPORT_ELEMENT_ORDER);
        if (Node.ID_REFERENCE.equals(sort))
            referenceIdButton.setSelection(true);
        else if (Node.ID_LOGICAL.equals(sort))
            logicalIdButton.setSelection(true);
        else
            sequenceIdButton.setSelection(true);

        filepathText = new Text(composite, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        gd.widthHint = 250;
        filepathText.setLayoutData(gd);
        filepathText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                filepath = filepathText.getText().trim();
                getButton(IDialogConstants.OK_ID).setEnabled(filepath.length() > 0);
            }
        });

        browseButton = new Button(composite, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                FileSaveDialog dlg = new FileSaveDialog(getShell());
                dlg.setFilterExtensions(new String[] { "*." + getFileExtension() });
                dlg.setFileName(getFileName());
                String filepath = dlg.open();
                if (filepath != null)
                    filepathText.setText(filepath);
            }
        });

        return composite;
    }

    private String getFileExtension() {
        if (jpegButton.getSelection())
            return FORMATS.get(JPG);
        else if (pngButton.getSelection())
            return FORMATS.get(PNG);
        else if (pdfButton.getSelection())
            return FORMATS.get(PDF);
        else if (rtfButton.getSelection())
            return FORMATS.get(RTF);
        else if (htmlButton.getSelection())
            return FORMATS.get(HTML);
        else if (docxButton.getSelection())
            return FORMATS.get(MS_WORD);
        else if (bpmnButton.getSelection())
            return FORMATS.get(BPMN2);
        else
            return "*";
    }

    private Set<String> getOptions() {
        Set<String> options = new HashSet<String>();
        if (docButton.getSelection())
            options.add(ExportHelper.DOCUMENTATION);
        if (attrsButton.getSelection())
            options.add(ExportHelper.ATTRIBUTES);
        if (varsButton.getSelection())
            options.add(ExportHelper.VARIABLES);
        return options;
    }

    private String getElementOrder() {
        if (referenceIdButton.getSelection())
            return Node.ID_REFERENCE;
        else if (logicalIdButton.getSelection())
            return Node.ID_LOGICAL;
        else
            return Node.ID_SEQUENCE;
    }

    private String getFileName() {
        return processVersion.getLabel() + "." + getFileExtension();
    }

    @Override
    protected void cancelPressed() {
        setReturnCode(CANCEL);
        close();
    }

    @Override
    protected void okPressed() {
        exportType = getFileExtension();
        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
            public void run() {
                Set<String> options = getOptions();
                try {
                    ExportHelper exporter = new ExportHelper(options, getElementOrder());
                    exporter.exportProcess(getFilepath(), exportType, flowchartPage.getProcess(),
                            flowchartPage.canvas);
                    IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
                    for (String format : FORMATS.keySet()) {
                        if (FORMATS.get(format).equals(exportType))
                            prefsStore.setValue(PreferenceConstants.PREFS_PROCESS_EXPORT_FORMAT,
                                    exportType);
                    }
                    prefsStore.setValue(PreferenceConstants.PREFS_PROCESS_EXPORT_DOCUMENTATION,
                            options.contains(ExportHelper.DOCUMENTATION));
                    prefsStore.setValue(PreferenceConstants.PREFS_PROCESS_EXPORT_ATTRIBUTES,
                            options.contains(ExportHelper.ATTRIBUTES));
                    prefsStore.setValue(PreferenceConstants.PREFS_PROCESS_EXPORT_VARIABLES,
                            options.contains(ExportHelper.VARIABLES));
                    prefsStore.setValue(PreferenceConstants.PREFS_PROCESS_EXPORT_ELEMENT_ORDER,
                            getElementOrder());
                }
                catch (Exception ex) {
                    PluginMessages.uiError(getShell(), ex, "Print Process");
                }
            }
        });

        setReturnCode(OK);
        close();
    }

    protected void createButtonsForButtonBar(Composite parent) {
        Button saveBtn = createButton(parent, IDialogConstants.OK_ID, "Save", false);
        saveBtn.setEnabled(false);
        createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", true);
    }
}
