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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IExportWizard;
import org.json.JSONException;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.common.utilities.timer.ActionCancelledException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.Exporter;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

public class ExportPackageWizard extends ImportExportWizard implements IExportWizard {
    ImportExportPage createPage() {
        return new ExportPackagePage();
    }

    void performImportExport(ProgressMonitor progressMonitor) throws IOException, JSONException,
            XmlException, DataAccessException, ActionCancelledException {
        Exporter exporter = new Exporter(getProject().getDesignerDataAccess());
        boolean exportJson = false;
        boolean includeTaskTemplates = false;
        boolean inferReferencedImpls = false;
        boolean exportZip = false;
        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
        if (getProject().isFilePersist()) {
            exportJson = prefsStore.getBoolean(PreferenceConstants.PREFS_EXPORT_JSON_FORMAT);
            includeTaskTemplates = !prefsStore
                    .getBoolean(PreferenceConstants.PREFS_SUPPRESS_TASK_TEMPLATES_IN_PKG_EXPORT);
            exportZip = prefsStore.getBoolean(PreferenceConstants.PREFS_EXPORT_ZIP_FORMAT);
        }
        else {
            inferReferencedImpls = !prefsStore.getBoolean(
                    PreferenceConstants.PREFS_SUPPRESS_INFER_REFERENCED_IMPLS_DURING_EXPORT);
        }

        List<WorkflowPackage> packages = getPackages();
        String export = null;
        if (exportZip) {
            File assetDir = getProject().getAssetDir();
            List<File> includes = new ArrayList<File>();
            for (WorkflowPackage pkg : packages)
                includes.add(new File(
                        assetDir + "/" + pkg.getName().replace('.', '/')));
            FileHelper.createZipFileWith(assetDir, new File(getPage().getFilePath()), includes);
        }
        else if (packages.size() == 1 && !exportJson)
            export = exporter.exportPackage(packages.get(0), includeTaskTemplates,
                    inferReferencedImpls, progressMonitor);
        else
            export = exporter.exportPackages(packages, exportJson, includeTaskTemplates,
                    progressMonitor);
        progressMonitor.progress(10);
        progressMonitor.subTask("Writing " + (exportJson ? "JSON" : "XML") + " file");
        if (export != null)
            writeFile(getPage().getFilePath(), export.getBytes());
        progressMonitor.progress(5);
    }

    protected void postRunUpdates() {
        getPackage().fireElementChangeEvent(ChangeType.STATUS_CHANGE,
                WorkflowPackage.STATUS_EXPORTED);
    }
}
