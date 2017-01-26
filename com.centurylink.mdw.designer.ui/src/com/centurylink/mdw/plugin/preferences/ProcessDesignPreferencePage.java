/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.preferences.model.MdwSettings;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

/**
 * Process Design preference page.
 */
public class ProcessDesignPreferencePage extends PreferencePage implements PreferenceConstants
{
  private Button allowAssetNamesWithoutExtensionsCheckbox;
  private Button inPlaceLabelEditingCheckbox;
  private Button compareConflictingAssetsCheckbox;
  private Button allowDeleteArchivedProcessesCheckbox;
  private Button doubleClickOpensSubprocsAndScriptsCheckbox;
  private Button inferSmartSubprocVersionSpecCheckbox;
  private Button showBamEventDataFieldCheckbox;
  private Button warnOverrideAttrsNotCarriedForwardCheckbox;
  private Button embeddedEditorForExcelCheckbox;
  private Button resetArtifactNagButton;
  private Text tempResourceLocationText;
  private Button loadScriptLibsOnEditCheckbox;
  private Spinner previousTempFileVersionsSpinner;
  private ColorDialog colorDialog;
  private Button colorDialogButton;
  private RGB readOnlyBackgroundRgb;

  public ProcessDesignPreferencePage()
  {
    super("Process Design");
  }

  protected Control createContents(Composite parent)
  {
    // allow delete archived processes
    allowDeleteArchivedProcessesCheckbox = createCheckbox(parent, "Allow Drag/Delete of Archived Processes and Assets", 3);

    // allow asset names without extensions
    allowAssetNamesWithoutExtensionsCheckbox = createCheckbox(parent, "Allow Asset Names without Extensions (MDW <= 5.2)", 3);

    // double click to open subprocs and scripts
    doubleClickOpensSubprocsAndScriptsCheckbox = createCheckbox(parent, "Double Click Opens Subprocesses and Scripts", 3);

    // double click to open subprocs and scripts
    inferSmartSubprocVersionSpecCheckbox = createCheckbox(parent, "Infer Smart Subprocess/Asset Version Spec (MDW >= 5.5)", 3);

    // compare conflicting assets during import
    compareConflictingAssetsCheckbox = createCheckbox(parent, "Compare Conflicting Assets during Import (MDW >= 5.2)", 3);

    // internal editor for excel
    embeddedEditorForExcelCheckbox = createCheckbox(parent, "Use Embedded Editor for Excel Spreadsheet Assets", 3);

    // in-place editing
    inPlaceLabelEditingCheckbox = createCheckbox(parent, "In-Place Label Editing (Requires Process Re-Open)", 3);

    // show the Event Data field on the BAM tab
    showBamEventDataFieldCheckbox = createCheckbox(parent, "Show BAM Event Data Input Field", 3);

    // warn when override attributes will not be carried forward
    warnOverrideAttrsNotCarriedForwardCheckbox = createCheckbox(parent, "Warn when Override Attributes not Carried Forward (VCS Assets)", 3);

    createSpacer(parent, 2, 1);

    Group resetGroup = new Group(parent, SWT.NONE);
    resetGroup.setText("Reminder prompts when saving temporary artifacts");
    GridLayout gl = new GridLayout();
    gl.numColumns = 2;
    resetGroup.setLayout(gl);
    GridData gd = new GridData();
    gd.horizontalAlignment = GridData.BEGINNING;
    gd.widthHint = 400;
    resetGroup.setLayoutData(gd);

    resetArtifactNagButton = createButton(resetGroup, " Reset ");
    resetArtifactNagButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
        String prefsKey = "MdwDocumentationSuppressSaveNag";
        prefsStore.setValue(prefsKey, false);
        prefsKey = "MdwJava CodeSuppressSaveNag";
        prefsStore.setValue(prefsKey, false);
        prefsKey = "MdwScriptSuppressSaveNag";
        prefsStore.setValue(prefsKey, false);
        prefsKey = "MdwTransformSuppressSaveNag";
        prefsStore.setValue(prefsKey, false);
      }
    });
    createLabel(resetGroup, "(Remind me to save related processes)", 1);

    createSpacer(parent, 2, 1);

    Group readOnlyGroup = new Group(parent, SWT.NONE);
    readOnlyGroup.setText("Background Color for Read-Only Processes");
    gl = new GridLayout();
    gl.numColumns = 2;
    readOnlyGroup.setLayout(gl);
    gd = new GridData();
    gd.horizontalAlignment = GridData.BEGINNING;
    gd.widthHint = 400;
    readOnlyGroup.setLayoutData(gd);

    colorDialog = new ColorDialog(getShell());
    colorDialog.setText("Select the background color for read-only processes.");
    colorDialogButton = createButton(readOnlyGroup, "Select...");
    colorDialogButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        RGB bgRgb = colorDialog.open();
        if (bgRgb != null)
          readOnlyBackgroundRgb = bgRgb;
      }
    });
    createLabel(readOnlyGroup, "(Requires Process Re-Open)", 1);

    createSpacer(parent, 2, 1);

    Group tempGroup = new Group(parent, SWT.NONE);
    tempGroup.setText("Workspace Temporary Resources");
    gl = new GridLayout();
    gl.numColumns = 2;
    tempGroup.setLayout(gl);
    gd = new GridData();
    gd.horizontalAlignment = GridData.BEGINNING;
    gd.widthHint = 400;
    tempGroup.setLayoutData(gd);

    // temp resource location
    createLabel(tempGroup, "Temp Location (relative to project folder)");
    tempResourceLocationText = createTextField(tempGroup, 250, 2);

    createSpacer(tempGroup, 2, 1);

    // previous temp file versions to retain
    previousTempFileVersionsSpinner = createSpinner(tempGroup, 1, 50);
    createLabel(tempGroup, "Archive Versions of Temp Files to Retain", 1);

    createSpacer(tempGroup, 2, 1);

    // auto load script libraries on edit
    loadScriptLibsOnEditCheckbox = createCheckbox(tempGroup, "Auto-Load Dynamic Java and Script Libraries on Edit", 2);

    initializeValues();

    return new Composite(parent, SWT.NULL);
  }

  protected void storeValues()
  {
    IPreferenceStore store = getPreferenceStore();

    store.setValue(PREFS_ALLOW_ASSETS_WITHOUT_EXTENSIONS, allowAssetNamesWithoutExtensionsCheckbox.getSelection());
    store.setValue(PREFS_IN_PLACE_LABEL_EDITING, inPlaceLabelEditingCheckbox.getSelection());
    store.setValue(PREFS_COMPARE_CONFLICTING_ASSETS, compareConflictingAssetsCheckbox.getSelection());
    store.setValue(PREFS_ALLOW_DELETE_ARCHIVED_PROCESSES, allowDeleteArchivedProcessesCheckbox.getSelection());
    store.setValue(PREFS_DOUBLE_CLICK_OPENS_SUBPROCESSES_AND_SCRIPTS, doubleClickOpensSubprocsAndScriptsCheckbox.getSelection());
    store.setValue(PREFS_INFER_SMART_SUBPROC_VERSION_SPEC, inferSmartSubprocVersionSpecCheckbox.getSelection());
    store.setValue(PREFS_SHOW_BAM_EVENT_DATA_INPUT_FIELD, showBamEventDataFieldCheckbox.getSelection());
    store.setValue(PREFS_WARN_OVERRIDE_ATTRS_NOT_CARRIED_FORWARD, warnOverrideAttrsNotCarriedForwardCheckbox.getSelection());
    store.setValue(PREFS_EMBEDDED_EDITOR_FOR_EXCEL, embeddedEditorForExcelCheckbox.getSelection());
    store.setValue(PREFS_READONLY_BG_RED, readOnlyBackgroundRgb.red);
    store.setValue(PREFS_READONLY_BG_GREEN, readOnlyBackgroundRgb.green);
    store.setValue(PREFS_READONLY_BG_BLUE, readOnlyBackgroundRgb.blue);
    store.setValue(PREFS_TEMP_RESOURCE_DIRECTORY, tempResourceLocationText.getText());
    store.setValue(PREFS_PREVIOUS_TEMP_FILE_VERSIONS_TO_KEEP, previousTempFileVersionsSpinner.getSelection());
    store.setValue(PREFS_LOAD_SCRIPT_LIBS_ON_EDIT, loadScriptLibsOnEditCheckbox.getSelection());
  }

  protected void initializeValues()
  {
    IPreferenceStore store = getPreferenceStore();
    allowAssetNamesWithoutExtensionsCheckbox.setSelection(store.getBoolean(PREFS_ALLOW_ASSETS_WITHOUT_EXTENSIONS));
    inPlaceLabelEditingCheckbox.setSelection(store.getBoolean(PREFS_IN_PLACE_LABEL_EDITING));
    compareConflictingAssetsCheckbox.setSelection(store.getBoolean(PREFS_COMPARE_CONFLICTING_ASSETS));
    allowDeleteArchivedProcessesCheckbox.setSelection(store.getBoolean(PREFS_ALLOW_DELETE_ARCHIVED_PROCESSES));
    doubleClickOpensSubprocsAndScriptsCheckbox.setSelection(store.getBoolean(PREFS_DOUBLE_CLICK_OPENS_SUBPROCESSES_AND_SCRIPTS));
    inferSmartSubprocVersionSpecCheckbox.setSelection(store.getBoolean(PREFS_INFER_SMART_SUBPROC_VERSION_SPEC));
    showBamEventDataFieldCheckbox.setSelection(store.getBoolean(PREFS_SHOW_BAM_EVENT_DATA_INPUT_FIELD));
    warnOverrideAttrsNotCarriedForwardCheckbox.setSelection(store.getBoolean(PREFS_WARN_OVERRIDE_ATTRS_NOT_CARRIED_FORWARD));
    embeddedEditorForExcelCheckbox.setSelection(store.getBoolean(PREFS_EMBEDDED_EDITOR_FOR_EXCEL));
    int red = store.getInt(PREFS_READONLY_BG_RED);
    int green = store.getInt(PREFS_READONLY_BG_GREEN);
    int blue = store.getInt(PREFS_READONLY_BG_BLUE);
    readOnlyBackgroundRgb = new RGB(red, green, blue);
    colorDialog.setRGB(readOnlyBackgroundRgb);
    tempResourceLocationText.setText(store.getString(PREFS_TEMP_RESOURCE_DIRECTORY));
    previousTempFileVersionsSpinner.setSelection(store.getInt(PREFS_PREVIOUS_TEMP_FILE_VERSIONS_TO_KEEP));
    loadScriptLibsOnEditCheckbox.setSelection(store.getBoolean(PREFS_LOAD_SCRIPT_LIBS_ON_EDIT));
  }

  protected void initializeFromDefaults()
  {
    IPreferenceStore store = getPreferenceStore();

    allowAssetNamesWithoutExtensionsCheckbox.setSelection(store.getDefaultBoolean(PREFS_ALLOW_ASSETS_WITHOUT_EXTENSIONS));
    inPlaceLabelEditingCheckbox.setSelection(store.getDefaultBoolean(PREFS_IN_PLACE_LABEL_EDITING));
    compareConflictingAssetsCheckbox.setSelection(store.getDefaultBoolean(PREFS_COMPARE_CONFLICTING_ASSETS));
    allowDeleteArchivedProcessesCheckbox.setSelection(store.getDefaultBoolean(PREFS_ALLOW_DELETE_ARCHIVED_PROCESSES));
    doubleClickOpensSubprocsAndScriptsCheckbox.setSelection(store.getDefaultBoolean(PREFS_DOUBLE_CLICK_OPENS_SUBPROCESSES_AND_SCRIPTS));
    inferSmartSubprocVersionSpecCheckbox.setSelection(store.getDefaultBoolean(PREFS_INFER_SMART_SUBPROC_VERSION_SPEC));
    showBamEventDataFieldCheckbox.setSelection(store.getDefaultBoolean(PREFS_SHOW_BAM_EVENT_DATA_INPUT_FIELD));
    warnOverrideAttrsNotCarriedForwardCheckbox.setSelection(store.getDefaultBoolean(PREFS_WARN_OVERRIDE_ATTRS_NOT_CARRIED_FORWARD));
    embeddedEditorForExcelCheckbox.setSelection(store.getDefaultBoolean(PREFS_EMBEDDED_EDITOR_FOR_EXCEL));
    int red = store.getDefaultInt(PREFS_READONLY_BG_RED);
    int green = store.getDefaultInt(PREFS_READONLY_BG_GREEN);
    int blue = store.getDefaultInt(PREFS_READONLY_BG_BLUE);
    readOnlyBackgroundRgb = new RGB(red, green, blue);
    colorDialog.setRGB(readOnlyBackgroundRgb);
    tempResourceLocationText.setText(store.getDefaultString(PREFS_TEMP_RESOURCE_DIRECTORY));
    previousTempFileVersionsSpinner.setSelection(store.getInt(PREFS_PREVIOUS_TEMP_FILE_VERSIONS_TO_KEEP));
    loadScriptLibsOnEditCheckbox.setSelection(store.getDefaultBoolean(PREFS_LOAD_SCRIPT_LIBS_ON_EDIT));
  }

  protected void setDefaultValues()
  {
    MdwSettings.setDefaultValues();
  }

  public boolean validate()
  {
    setErrorMessage(null);
    setValid(true);
    return true;
  }

}