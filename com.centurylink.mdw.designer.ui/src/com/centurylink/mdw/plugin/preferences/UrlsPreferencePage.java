/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.preferences.model.MdwSettings;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

/**
 * MDW URLs preference page.
 */
public class UrlsPreferencePage extends PreferencePage implements PreferenceConstants {
    public static final String PREF_PAGE_ID = "mdw.preferences.UrlsPreferencePage";

    private Text mdwReleasesUrlTextField;
    private Button includePreviewReleasesCheckbox;
    private Text discoveryUrlTextField;
    private Text workspaceSetupUrlTextField;
    private Text httpConnectTimeoutText;
    private Text httpReadTimeoutText;
    private Text smtpHostTextField;
    private Text smtpPortTextField;

    public UrlsPreferencePage() {
        super("URLs");
    }

    protected Control createContents(Composite parent) {
        // discovery url
        createLabel(parent, "Discovery URL:", 1);
        discoveryUrlTextField = createTextField(parent, 330, 2);
        discoveryUrlTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validate();
            }
        });

        // mdw releases url
        createLabel(parent, "MDW Releases URL:", 1);
        mdwReleasesUrlTextField = createTextField(parent, 330, 2);
        mdwReleasesUrlTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validate();
            }
        });

        // preview releases
        includePreviewReleasesCheckbox = createCheckbox(parent,
                "Include Preview Builds and Snapshots", 3);

        createSpacer(parent);

        // workspace setup releases url
        createLabel(parent, "Workspace Setup URL:", 1);
        workspaceSetupUrlTextField = createTextField(parent, 330, 2);
        workspaceSetupUrlTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validate();
            }
        });

        createSpacer(parent);

        // http timeouts
        createLabel(parent, "HTTP Timeouts in Milliseconds (Requires Restart):", 3);
        Composite timeoutSettings = createComposite(parent, 5, 3);

        createLabel(timeoutSettings, "Connect:", 1);
        httpConnectTimeoutText = createTextField(timeoutSettings, 50, 1);
        httpConnectTimeoutText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validate();
            }
        });
        createLabel(timeoutSettings, "   ", 1);
        createLabel(timeoutSettings, "Read:", 1);
        httpReadTimeoutText = createTextField(timeoutSettings, 50, 1);
        httpReadTimeoutText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validate();
            }
        });

        createSpacer(parent);

        // smtp mail settings
        createLabel(parent, "Error Reporting SMTP Settings:", 3);
        Composite mailSettings = createComposite(parent, 5, 3);

        createLabel(mailSettings, "Host:", 1);
        smtpHostTextField = createTextField(mailSettings, 200, 1);
        smtpHostTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validate();
            }
        });
        createLabel(mailSettings, "   ", 1);
        createLabel(mailSettings, "Port:", 1);
        smtpPortTextField = createTextField(mailSettings, 30, 1);
        smtpPortTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validate();
            }
        });

        initializeValues();

        return new Composite(parent, SWT.NULL);
    }

    protected void storeValues() {
        IPreferenceStore store = getPreferenceStore();

        store.setValue(PREFS_MDW_RELEASES_URL, mdwReleasesUrlTextField.getText().trim());
        store.setValue(PREFS_INCLUDE_PREVIEW_BUILDS, includePreviewReleasesCheckbox.getSelection());
        store.setValue(PREFS_WORKSPACE_SETUP_URL, workspaceSetupUrlTextField.getText().trim());
        store.setValue(PREFS_DISCOVERY_URL, discoveryUrlTextField.getText().trim());
        store.setValue(PREFS_HTTP_CONNECT_TIMEOUT_MS,
                Integer.parseInt(httpConnectTimeoutText.getText()));
        store.setValue(PREFS_HTTP_READ_TIMEOUT_MS, Integer.parseInt(httpReadTimeoutText.getText()));
        store.setValue(PREFS_SMTP_HOST, smtpHostTextField.getText().trim());
        store.setValue(PREFS_SMTP_PORT, Integer.parseInt(smtpPortTextField.getText().trim()));
    }

    protected void initializeValues() {
        IPreferenceStore store = getPreferenceStore();

        mdwReleasesUrlTextField.setText(store.getString(PREFS_MDW_RELEASES_URL));
        includePreviewReleasesCheckbox.setSelection(store.getBoolean(PREFS_INCLUDE_PREVIEW_BUILDS));
        workspaceSetupUrlTextField.setText(store.getString(PREFS_WORKSPACE_SETUP_URL));
        discoveryUrlTextField.setText(store.getString(PREFS_DISCOVERY_URL));
        httpConnectTimeoutText.setText(String.valueOf(store.getInt(PREFS_HTTP_CONNECT_TIMEOUT_MS)));
        httpReadTimeoutText.setText(String.valueOf(store.getInt(PREFS_HTTP_READ_TIMEOUT_MS)));
        smtpHostTextField.setText(store.getString(PREFS_SMTP_HOST));
        smtpPortTextField.setText(String.valueOf(store.getInt(PREFS_SMTP_PORT)));
    }

    protected void initializeFromDefaults() {
        IPreferenceStore store = getPreferenceStore();

        mdwReleasesUrlTextField.setText(store.getDefaultString(PREFS_MDW_RELEASES_URL));
        workspaceSetupUrlTextField.setText(store.getDefaultString(PREFS_WORKSPACE_SETUP_URL));
        discoveryUrlTextField.setText(store.getDefaultString(PREFS_DISCOVERY_URL));
        httpConnectTimeoutText
                .setText(String.valueOf(store.getDefaultInt(PREFS_HTTP_CONNECT_TIMEOUT_MS)));
        httpReadTimeoutText
                .setText(String.valueOf(store.getDefaultInt(PREFS_HTTP_READ_TIMEOUT_MS)));
        smtpHostTextField.setText(store.getDefaultString(PREFS_SMTP_HOST));
        smtpPortTextField.setText(String.valueOf(store.getDefaultInt(PREFS_SMTP_PORT)));
    }

    protected void setDefaultValues() {
        MdwSettings.setDefaultValues();
    }

    public boolean validate() {
        if (!checkUrl(mdwReleasesUrlTextField.getText().trim())) {
            setErrorMessage("Invalid MDW Releases URL.");
            setValid(false);
            return false;
        }
        if (!checkUrl(workspaceSetupUrlTextField.getText().trim())) {
            setErrorMessage("Invalid Workspace Setup URL.");
            setValid(false);
            return false;
        }
        if (!checkString(discoveryUrlTextField.getText().trim())) {
            setErrorMessage("Invalid Discovery URL.");
            setValid(false);
            return false;
        }
        if (!checkInt(httpConnectTimeoutText.getText().trim())) {
            setErrorMessage("Invalid HTTP Connect Timeout.");
            setValid(false);
            return false;
        }
        if (!checkInt(httpReadTimeoutText.getText().trim())) {
            setErrorMessage("Invalid HTTP Read Timeout.");
            setValid(false);
            return false;
        }
        if (!checkString(smtpHostTextField.getText().trim())) {
            setErrorMessage("Missing SMTP Host.");
            setValid(false);
            return false;
        }
        if (!checkInt(smtpPortTextField.getText().trim())) {
            setErrorMessage("Invalid SMTP Port.");
            setValid(false);
            return false;
        }

        setErrorMessage(null);
        setValid(true);
        return true;
    }
}