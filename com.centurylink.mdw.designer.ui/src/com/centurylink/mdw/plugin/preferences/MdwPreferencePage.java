/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.preferences.model.MdwSettings;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

/**
 * Main MDW preference page.
 */
public class MdwPreferencePage extends PreferencePage implements PreferenceConstants
{
  private Combo mdwReportingLevelCombo;
  private Button logTimingsCheckbox;
  private Button logConnectErrorsCheckbox;
  private Text jdbcFetchSizeText;
  private Text copyrightNoticeTextArea;
  private Button eventManagerCheckbox;
  private Button threadPoolManagerCheckbox;
  private Button useDiscoveredVcsCredsCheckbox;

  public MdwPreferencePage()
  {
    super("MDW");
  }

  protected Control createContents(Composite parent)
  {
    // mdw reporting level
    createLabel(parent, "MDW Reporting Level:", 1);
    List<String> reportingLevels = new ArrayList<String>();
    for (Integer level : PluginMessages.MESSAGE_LEVELS.keySet())
      reportingLevels.add(level + " - " + PluginMessages.MESSAGE_LEVELS.get(level));
    Collections.sort(reportingLevels, new Comparator<String>()
    {
      public int compare(String level1, String level2)
      {
        Integer int1 = new Integer(level1.substring(0, level1.indexOf('-') - 1));
        Integer int2 = new Integer(level2.substring(0, level2.indexOf('-') - 1));
        return int1.compareTo(int2);
      }
    });
    mdwReportingLevelCombo = createCombo(parent, reportingLevels, 150);
    mdwReportingLevelCombo.setVisibleItemCount(6);

    createSpacer(parent);

    // log timings
    logTimingsCheckbox = createCheckbox(parent, "Log timings to Event Log", 3);
    // log connect errors
    logConnectErrorsCheckbox = createCheckbox(parent, "Log routine connection/translation errors", 3);

    createSpacer(parent, 1, 2);

    // jdbc fetch size
    Composite c = new Composite(parent, SWT.NULL);
    GridLayout gl = new GridLayout(2, false);
    gl.marginWidth = 0;
    c.setLayout(gl);
    createLabel(c, "JDBC Fetch Size:", 1);
    jdbcFetchSizeText = createIntTextField(c, 75, 1);
    jdbcFetchSizeText.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        validate();
      }
    });

    createSpacer(parent, 1, 2);

    // copyright
    createLabel(parent, "Copyright Notice for Generated Code:", 3);
    copyrightNoticeTextArea = createTextArea(parent, 50, 2);

    createSpacer(parent);

    // swing launch
    Group swingGroup = createGroup(parent, "Launchable Swing Tools", 1, 3);
    eventManagerCheckbox = createCheckbox(swingGroup, "Event Manager", 1);
    threadPoolManagerCheckbox = createCheckbox(swingGroup, "Thread Pool Manager", 1);

    createSpacer(parent);
    useDiscoveredVcsCredsCheckbox = createCheckbox(parent, "Use discovered VCS credentials if available", 1);

    initializeValues();

    return new Composite(parent, SWT.NULL);
  }

  protected void storeValues()
  {
    IPreferenceStore store = getPreferenceStore();

    String selectedLevel = mdwReportingLevelCombo.getText();
    Integer level = new Integer(selectedLevel.substring(0, selectedLevel.indexOf('-') - 1));
    store.setValue(PREFS_MDW_REPORTING_LEVEL, level.intValue());
    store.setValue(PREFS_LOG_TIMINGS, logTimingsCheckbox.getSelection());
    store.setValue(PREFS_LOG_CONNECT_ERRORS, logConnectErrorsCheckbox.getSelection());
    store.setValue(PREFS_JDBC_FETCH_SIZE, jdbcFetchSizeText.getText().trim());
    store.setValue(PREFS_COPYRIGHT_NOTICE, copyrightNoticeTextArea.getText().trim());
    store.setValue(PREFS_SWING_LAUNCH_EVENT_MANAGER, eventManagerCheckbox.getSelection());
    store.setValue(PREFS_SWING_LAUNCH_THREAD_POOL_MANAGER, threadPoolManagerCheckbox.getSelection());
    store.setValue(PREFS_USE_DISCOVERED_VCS_CREDS, useDiscoveredVcsCredsCheckbox.getSelection());
  }

  protected void initializeValues()
  {
    IPreferenceStore store = getPreferenceStore();

    int reportingLevel = store.getInt(PREFS_MDW_REPORTING_LEVEL);
    mdwReportingLevelCombo.setText(reportingLevel + " - " + PluginMessages.MESSAGE_LEVELS.get(new Integer(reportingLevel)));
    logTimingsCheckbox.setSelection(store.getBoolean(PREFS_LOG_TIMINGS));
    logConnectErrorsCheckbox.setSelection(store.getBoolean(PREFS_LOG_CONNECT_ERRORS));
    copyrightNoticeTextArea.setText(store.getString(PREFS_COPYRIGHT_NOTICE));
    jdbcFetchSizeText.setText(String.valueOf(store.getInt(PREFS_JDBC_FETCH_SIZE)));
    eventManagerCheckbox.setSelection(store.getBoolean(PREFS_SWING_LAUNCH_EVENT_MANAGER));
    threadPoolManagerCheckbox.setSelection(store.getBoolean(PREFS_SWING_LAUNCH_THREAD_POOL_MANAGER));
    useDiscoveredVcsCredsCheckbox.setSelection(store.getBoolean(PREFS_USE_DISCOVERED_VCS_CREDS));
  }

  protected void initializeFromDefaults()
  {
    IPreferenceStore store = getPreferenceStore();

    int defaultReportingLevel = store.getDefaultInt(PREFS_MDW_REPORTING_LEVEL);
    mdwReportingLevelCombo.setText(PluginMessages.MESSAGE_LEVELS.get(new Integer(defaultReportingLevel)));
    jdbcFetchSizeText.setText(String.valueOf(store.getDefaultInt(PREFS_JDBC_FETCH_SIZE)));
    copyrightNoticeTextArea.setText(store.getDefaultString(PREFS_COPYRIGHT_NOTICE));
    eventManagerCheckbox.setSelection(true);
    threadPoolManagerCheckbox.setSelection(true);
  }

  protected void setDefaultValues()
  {
    MdwSettings.setDefaultValues();
  }

  public boolean validate()
  {
    if (jdbcFetchSizeText.getText().trim().isEmpty())
    {
      setErrorMessage("JDBC Fetch Size is required");
      setValid(false);
      return false;
    }
    setErrorMessage(null);
    setValid(true);
    return true;
  }

}