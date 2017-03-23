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
package com.centurylink.mdw.plugin.designer.properties.value;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.dialogs.FrameworkUpdateDialog;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditorList;
import com.centurylink.mdw.plugin.project.assembly.ProjectConfigurator;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;

public class ScriptEditorValueProvider extends ArtifactEditorValueProvider {
    private Activity activity;

    public ScriptEditorValueProvider(Activity activity) {
        super(activity);
        this.activity = activity;
    }

    public byte[] getArtifactContent() {
        String value = activity.getAttribute(getAttributeName());
        return value == null ? null : value.getBytes();
    }

    public String getArtifactTypeDescription() {
        return "Script";
    }

    @Override
    public boolean beforeTempFileOpened() {
        if (isGroovy()) {
            ProjectConfigurator projConf = new ProjectConfigurator(getProject(),
                    MdwPlugin.getSettings());
            projConf.setGroovy(new NullProgressMonitor());

            try {
                if (getProject().isRemote() && projConf.isJavaCapable()
                        && !projConf.hasFrameworkJars()) {
                    FrameworkUpdateDialog updateDlg = new FrameworkUpdateDialog(
                            MdwPlugin.getShell(), MdwPlugin.getSettings(), getProject());
                    if (updateDlg.open() == Dialog.OK) {
                        String origVer = getProject().getMdwVersion(); // as
                                                                       // reported
                                                                       // by
                                                                       // server
                                                                       // or db
                        getProject().setMdwVersion(updateDlg.getMdwVersion()); // for
                                                                               // downloading
                        projConf.initializeFrameworkJars();
                        getProject().setMdwVersion(origVer);
                    }
                }
            }
            catch (CoreException ex) {
                PluginMessages.uiError(ex, "Framework Jars", getProject());
            }

        }
        return true;
    }

    @Override
    public void afterTempFileSaved() {
        if (isGroovy() && !activity.getProject().checkRequiredVersion(5, 5)) {
            // activity level bsn
            String bsn = getBundleSymbolicName();
            if (bsn != null)
                activity.setAttribute(WorkAttributeConstant.OSGI_BSN, bsn);
        }
    }

    public boolean isGroovy() {
        return "Groovy".equalsIgnoreCase(getLanguage());
    }

    public void valueChanged(String newValue) {
        activity.setAttribute(getAttributeName(), newValue);
    }

    public String getEditLinkLabel() {
        byte[] content = getArtifactContent();
        boolean hasContent = content != null && content.length > 0;
        String label = activity.isReadOnly() ? "<A>View Script</A>" : "<A>Edit Script</A>";
        if (hasContent)
            label += " *";

        return label;
    }

    public List<String> getLanguageOptions() {
        return activity.getScriptLanguages();
    }

    public String getDefaultLanguage() {
        return "Groovy";
    }

    public String getLanguage() {
        return activity.getScriptLanguage();
    }

    public void languageChanged(String newLanguage) {
        activity.setScriptLanguage(newLanguage);
    }

    public String getAttributeName() {
        PropertyEditorList propEditorList = new PropertyEditorList(activity);
        for (PropertyEditor propertyEditor : propEditorList) {
            if (propertyEditor.getType().equals(PropertyEditor.TYPE_SCRIPT)) {
                return propertyEditor.getName();
            }
        }
        return "RULE";
    }
}