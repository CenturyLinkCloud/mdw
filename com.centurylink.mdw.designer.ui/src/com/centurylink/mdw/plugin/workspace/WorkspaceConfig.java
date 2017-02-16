/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.workspace;

import org.eclipse.jface.preference.IPreferenceStore;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.preferences.model.MdwSettings;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

public class WorkspaceConfig implements PreferenceConstants {
    public static final String[] CODE_FORMATTERS = { "CenturyLink IT Code Formatter",
            "MDW Code Formatter" };
    public static final String[] CODE_TEMPLATES = { "CenturyLink IT Code Templates",
            "MDW Code Templates" };

    private MdwSettings mdwSettings;

    public MdwSettings getMdwSettings() {
        return mdwSettings;
    }

    public WorkspaceConfig(MdwSettings mdwSettings) {
        this.mdwSettings = mdwSettings;
        IPreferenceStore store = MdwPlugin.getDefault().getPreferenceStore();
        String currentTemplates = store.getString(PREFS_CURRENT_CODE_TEMPLATES);
        if (currentTemplates != null && currentTemplates.trim().length() != 0)
            this.codeTemplates = currentTemplates;
        String currentFormatter = store.getString(PREFS_CURRENT_CODE_FORMATTER);
        if (currentFormatter != null && currentFormatter.trim().length() != 0)
            this.codeFormatter = currentFormatter;
    }

    private String codeFormatter = CODE_FORMATTERS[0];

    public String getCodeFormatter() {
        return codeFormatter;
    }

    public void setCodeFormatter(String formatter) {
        this.codeFormatter = formatter;
    }

    private String codeTemplates = CODE_TEMPLATES[0];

    public String getCodeTemplates() {
        return codeTemplates;
    }

    public void setCodeTemplates(String templates) {
        this.codeTemplates = templates;
    }

    private boolean eclipseAutobuild;

    public boolean isEclipseAutobuild() {
        return eclipseAutobuild;
    }

    public void setEclipseAutobuild(boolean b) {
        eclipseAutobuild = b;
    }

    /**
     * Saves the code formatter and templates in the preference store.
     */
    public void save() {
        IPreferenceStore store = MdwPlugin.getDefault().getPreferenceStore();
        store.setValue(PREFS_CURRENT_CODE_TEMPLATES, this.codeTemplates);
        store.setValue(PREFS_CURRENT_CODE_FORMATTER, this.codeFormatter);
    }

    public String toString() {
        return "codeFormatter: " + getCodeFormatter() + "\n" + "codeTemplates: "
                + getCodeTemplates() + "\n" + "eclipseAutobuild: " + isEclipseAutobuild() + "\n";
    }

}
