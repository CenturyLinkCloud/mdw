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

import java.io.IOException;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.timer.ActionCancelledException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.designer.utils.ValidationException;
import com.centurylink.mdw.plugin.designer.Importer;

public class ImportAttributesWizard extends ImportExportWizard {
    private String prefix;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    ImportExportPage createPage() {
        return new ImportAttributesPage();
    }

    void performImportExport(ProgressMonitor progressMonitor) throws IOException, XmlException,
            DataAccessException, ValidationException, ActionCancelledException {
        progressMonitor.progress(10);
        progressMonitor.subTask("Reading XML file");
        byte[] bytes = readFile(getPage().getFilePath());
        progressMonitor.progress(20);
        if (progressMonitor.isCanceled())
            throw new ActionCancelledException();
        progressMonitor.subTask("Performing Import");
        Importer importer = new Importer(getProject().getDataAccess(), getShell());
        importer.importAttributes(getProcess() == null ? getPackage() : getProcess(),
                new String(bytes), progressMonitor, prefix);
        getProject().getDesignerProxy().getCacheRefresh().doRefresh(true);
        progressMonitor.progress(30);
    }
}
