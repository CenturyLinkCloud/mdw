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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;

public class ImportAssetPage extends ImportExportPage {
    public ImportAssetPage() {
        super("Import MDW Asset", "Import a workflow asset from a file.");
    }

    protected void createControls(Composite composite, int ncol) {
        super.createControls(composite, ncol);
        createCommentsControls(composite, ncol);

        ImportExportWizard wizard = (ImportExportWizard) getWizard();
        WorkflowElement we = wizard.getElement();

        if (we != null && !we.getProject().isFilePersist())
            createLockControls(composite, ncol);
    }

    protected String getFileExtension() {
        if (getAsset() == null)
            return ".*";
        else
            return getAsset().getExtension();
    }

    boolean isPageValid() {
        return super.isPageValid() && (!getProject().isProduction() || getComments().length() > 0)
                && getExistingAssetEditor() == null && new File(getFilePath()).exists()
                && (getAsset() != null || getLanguageFromExtension() != null);
    }

    public IStatus[] getStatuses() {
        IStatus[] statuses = super.getStatuses();
        if (statuses != null)
            return statuses;
        String msg = null;

        IEditorPart existingAssetEditor = getExistingAssetEditor();
        if (existingAssetEditor != null)
            msg = "'" + existingAssetEditor.getTitle()
                    + "' is currently open in an editor.  Please save and close before importing.";
        else if (getProject().isProduction() && getComments().length() == 0)
            msg = "Please enter revision comments";
        else if (!(new File(getFilePath()).exists()))
            msg = "File does not exist: " + getFilePath();
        else if (getAsset() == null && getLanguageFromExtension() == null)
            msg = "Unable to infer asset format from filename extension";

        if (msg == null)
            return null;
        else
            return new IStatus[] { new Status(IStatus.ERROR, getPluginId(), 0, msg, null) };
    }

    private IEditorPart getExistingAssetEditor() {
        WorkflowAsset existingAsset = getAsset();
        if (existingAsset == null) // package selected, not asset
            existingAsset = getPackage().getAsset(new File(getFilePath()).getName());
        return existingAsset == null ? null : existingAsset.findOpenEditor();
    }

    private String getLanguageFromExtension() {
        File file = new File(getFilePath());
        int lastDot = file.getName().lastIndexOf('.');
        if (lastDot == -1)
            return null;
        return RuleSetVO.getLanguage(file.getName().substring(lastDot));
    }
}
