/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.dialogs;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.Template;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

public class TemplateRunDialog extends TrayDialog {
    private Template template;

    private Button inputFileRadio;
    private Button inputDirRadio;
    private Text inputFileText;
    private Text inputDirText;
    private Button inputFileBrowseButton;
    private Button inputDirBrowseButton;
    private Text outputLocationText;
    private Button outputLocationBrowseButton;
    private Text velocityPropertiesFileText;
    private Button velocityPropertiesFileButton;
    private Text velocityToolboxFileText;
    private Button velocityToolboxFileButton;

    private String inputFilePath;

    public String getInputFilePath() {
        return inputFilePath;
    }

    private String inputDirPath;

    public String getInputDirPath() {
        return inputDirPath;
    }

    private String outputDirPath;

    public String getOutputDirPath() {
        return outputDirPath;
    }

    private String velocityPropFilePath;

    public String getVelocityPropFilePath() {
        return velocityPropFilePath;
    }

    public String velocityToolsFilePath;

    public String getVelocityToolsFilePath() {
        return velocityToolsFilePath;
    }

    public TemplateRunDialog(Shell shell, Template template) {
        super(shell);
        this.template = template;

        // initial output location
        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
        inputFilePath = prefsStore.getString(PreferenceConstants.PREFS_TEMPLATE_INPUT_FILE);
        if (inputFilePath.length() == 0)
            inputFilePath = null;
        inputDirPath = prefsStore.getString(PreferenceConstants.PREFS_TEMPLATE_INPUT_LOCATION);
        if (inputDirPath.length() == 0)
            inputDirPath = null;
        outputDirPath = prefsStore.getString(PreferenceConstants.PREFS_TEMPLATE_OUTPUT_LOCATION);
        velocityPropFilePath = prefsStore
                .getString(PreferenceConstants.PREFS_VELOCITY_PROPERTY_FILE_LOCATION);
        velocityToolsFilePath = prefsStore
                .getString(PreferenceConstants.PREFS_VELOCITY_TOOLBOX_FILE_LOCATION);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.getShell().setText("Run Template");
        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        composite.setLayout(gl);

        createInputControls(composite, 2);
        createOutputControls(composite, 2);
        createConfigControls(composite, 2);
        updateButtonEnablement();

        return composite;
    }

    private void createInputControls(Composite parent, int ncol) {
        boolean inputIsFile = inputDirPath == null;

        Group inputGroup = new Group(parent, SWT.NONE);
        inputGroup.setText("Input");
        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        inputGroup.setLayout(gl);
        GridData gd = new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = 2;
        inputGroup.setLayoutData(gd);

        // file input
        inputFileRadio = new Button(inputGroup, SWT.RADIO | SWT.LEFT);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 2;
        inputFileRadio.setLayoutData(gd);
        inputFileRadio.setSelection(inputIsFile);
        inputFileRadio.setText("Select File");
        inputFileRadio.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean selected = inputFileRadio.getSelection();
                inputDirRadio.setSelection(!selected);
                enableInputDirControls(!selected);
                enableInputFileControls(selected);
            }
        });

        Label lbl = new Label(inputGroup, SWT.NONE);
        lbl.setText("Property File:");
        gd = new GridData(SWT.LEFT);
        gd.horizontalSpan = 2;
        gd.horizontalIndent = 20;
        lbl.setLayoutData(gd);
        inputFileText = new Text(inputGroup, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(SWT.LEFT);
        gd.widthHint = 450;
        gd.horizontalIndent = 20;
        inputFileText.setLayoutData(gd);
        if (inputFilePath != null)
            inputFileText.setText(inputFilePath);
        inputFileText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                inputFilePath = inputFileText.getText().trim();
                updateButtonEnablement();
            }
        });

        inputFileBrowseButton = new Button(inputGroup, SWT.PUSH);
        inputFileBrowseButton.setText("Browse...");
        inputFileBrowseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                FileDialog dlg = new FileDialog(getShell());
                String sel = dlg.open();
                if (sel != null)
                    inputFileText.setText(sel);
            }
        });

        enableInputFileControls(inputIsFile);

        // dir input
        inputDirRadio = new Button(inputGroup, SWT.RADIO | SWT.LEFT);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 2;
        inputDirRadio.setLayoutData(gd);
        inputDirRadio.setSelection(!inputIsFile);
        inputDirRadio.setText("Select Directory");
        inputDirRadio.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean selected = inputDirRadio.getSelection();
                inputFileRadio.setSelection(!selected);
                enableInputFileControls(!selected);
                enableInputDirControls(selected);
            }
        });

        lbl = new Label(inputGroup, SWT.NONE);
        lbl.setText("Property File Location:");
        gd = new GridData(SWT.LEFT);
        gd.horizontalSpan = 2;
        gd.horizontalIndent = 20;
        lbl.setLayoutData(gd);
        inputDirText = new Text(inputGroup, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(SWT.LEFT);
        gd.widthHint = 450;
        gd.horizontalIndent = 20;
        inputDirText.setLayoutData(gd);
        if (inputDirPath != null)
            inputDirText.setText(inputDirPath);
        inputDirText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                inputDirPath = inputDirText.getText().trim();
                updateButtonEnablement();
            }
        });

        inputDirBrowseButton = new Button(inputGroup, SWT.PUSH);
        inputDirBrowseButton.setText("Browse...");
        inputDirBrowseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dlg = new DirectoryDialog(getShell());
                if (inputDirPath != null)
                    dlg.setFilterPath(inputDirPath);
                String sel = dlg.open();
                if (sel != null)
                    inputDirText.setText(sel);
            }
        });

        enableInputDirControls(!inputIsFile);
    }

    private void createOutputControls(Composite parent, int ncol) {
        Group outputGroup = new Group(parent, SWT.NONE);
        outputGroup.setText("Output");
        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        outputGroup.setLayout(gl);
        GridData gd = new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = 2;
        gd.verticalIndent = 3;
        outputGroup.setLayoutData(gd);

        Label lbl = new Label(outputGroup, SWT.NONE);
        lbl.setText("Location:");
        gd = new GridData(SWT.LEFT);
        gd.horizontalSpan = 2;
        gd.horizontalIndent = 20;
        lbl.setLayoutData(gd);
        outputLocationText = new Text(outputGroup, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(SWT.LEFT);
        gd.widthHint = 450;
        gd.horizontalIndent = 20;
        outputLocationText.setLayoutData(gd);

        outputLocationText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                outputDirPath = outputLocationText.getText().trim();
                updateButtonEnablement();
            }
        });
        if (outputDirPath != null)
            outputLocationText.setText(outputDirPath);

        outputLocationBrowseButton = new Button(outputGroup, SWT.PUSH);
        outputLocationBrowseButton.setText("Browse...");
        outputLocationBrowseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dlg = new DirectoryDialog(getShell());
                if (outputDirPath != null)
                    dlg.setFilterPath(outputDirPath);
                String sel = dlg.open();
                if (sel != null)
                    outputLocationText.setText(sel);
            }
        });
    }

    private void createConfigControls(Composite parent, int ncol) {
        Group configGroup = new Group(parent, SWT.NONE);
        configGroup.setText("Config (Optional)");
        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        configGroup.setLayout(gl);
        GridData gd = new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = 2;
        gd.verticalIndent = 3;
        configGroup.setLayoutData(gd);

        Label lbl = new Label(configGroup, SWT.NONE);
        lbl.setText("Velocity Property File:");
        gd = new GridData(SWT.LEFT);
        gd.horizontalSpan = 2;
        gd.horizontalIndent = 20;
        lbl.setLayoutData(gd);

        velocityPropertiesFileText = new Text(configGroup, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(SWT.LEFT);
        gd.widthHint = 450;
        gd.horizontalIndent = 20;
        velocityPropertiesFileText.setLayoutData(gd);
        velocityPropertiesFileText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                velocityPropFilePath = velocityPropertiesFileText.getText().trim();
            }
        });
        if (velocityPropFilePath != null)
            velocityPropertiesFileText.setText(velocityPropFilePath);

        velocityPropertiesFileButton = new Button(configGroup, SWT.PUSH);
        velocityPropertiesFileButton.setText("Browse...");
        velocityPropertiesFileButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                FileDialog dlg = new FileDialog(getShell());
                String sel = dlg.open();
                if (sel != null)
                    velocityPropertiesFileText.setText(sel);
            }
        });

        lbl = new Label(configGroup, SWT.NONE);
        lbl.setText("Velocity Toolbox File:");
        gd = new GridData(SWT.LEFT);
        gd.horizontalSpan = 2;
        gd.horizontalIndent = 20;
        lbl.setLayoutData(gd);
        velocityToolboxFileText = new Text(configGroup, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(SWT.LEFT);
        gd.widthHint = 450;
        gd.horizontalIndent = 20;
        velocityToolboxFileText.setLayoutData(gd);
        velocityToolboxFileText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                velocityToolsFilePath = velocityToolboxFileText.getText().trim();
            }
        });
        if (velocityToolsFilePath != null)
            velocityToolboxFileText.setText(velocityToolsFilePath);

        velocityToolboxFileButton = new Button(configGroup, SWT.PUSH);
        velocityToolboxFileButton.setText("Browse...");
        velocityToolboxFileButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                FileDialog dlg = new FileDialog(getShell());
                String sel = dlg.open();
                if (sel != null)
                    velocityToolboxFileText.setText(sel);
            }
        });
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        Control buttonBar = super.createButtonBar(parent);
        updateButtonEnablement();
        return buttonBar;
    }

    @Override
    protected void okPressed() {
        try {
            IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
            prefsStore.setValue(PreferenceConstants.PREFS_TEMPLATE_INPUT_FILE, inputFilePath);
            prefsStore.setValue(PreferenceConstants.PREFS_TEMPLATE_INPUT_LOCATION, inputDirPath);
            prefsStore.setValue(PreferenceConstants.PREFS_TEMPLATE_OUTPUT_LOCATION, outputDirPath);
            prefsStore.setValue(PreferenceConstants.PREFS_VELOCITY_PROPERTY_FILE_LOCATION,
                    velocityPropFilePath);
            prefsStore.setValue(PreferenceConstants.PREFS_VELOCITY_TOOLBOX_FILE_LOCATION,
                    velocityToolsFilePath);

            String input = inputFilePath == null || inputFilePath.length() == 0 ? inputDirPath
                    : inputFilePath;
            template.runWith(input, outputDirPath, velocityPropFilePath, velocityToolsFilePath);
        }
        catch (Exception ex) {
            PluginMessages.uiError(getShell(), ex, "Run Template", template.getProject());
        }

        super.okPressed();
    }

    private void updateButtonEnablement() {
        boolean enabled = true;
        if (inputFileRadio.getSelection()) {
            if (inputFilePath == null || inputFilePath.length() == 0)
                enabled = false;
        }
        else {
            if (inputDirPath == null || inputDirPath.length() == 0)
                enabled = false;
        }

        if (outputDirPath == null || outputDirPath.length() == 0)
            enabled = false;

        Button okButton = getButton(OK);
        if (okButton != null)
            okButton.setEnabled(enabled);
    }

    private void enableInputFileControls(boolean enabled) {
        if (inputFileText.isEnabled() != enabled) {
            if (!enabled) {
                inputFilePath = null;
                inputFileText.setText("");
            }
            inputFileText.setEnabled(enabled);
            inputFileBrowseButton.setEnabled(enabled);
        }
    }

    private void enableInputDirControls(boolean enabled) {
        if (inputDirText.isEnabled() != enabled) {
            if (!enabled) {
                inputDirPath = null;
                inputDirText.setText("");
            }
            inputDirText.setEnabled(enabled);
            inputDirBrowseButton.setEnabled(enabled);
        }
    }
}