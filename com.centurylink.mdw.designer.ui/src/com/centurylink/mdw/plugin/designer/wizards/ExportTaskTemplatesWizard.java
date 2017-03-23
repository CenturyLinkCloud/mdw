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
import org.eclipse.ui.IExportWizard;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.timer.ActionCancelledException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.plugin.designer.Exporter;

public class ExportTaskTemplatesWizard extends ImportExportWizard implements IExportWizard {
    ImportExportPage createPage() {
        return new ExportTaskTemplatesPage();
    }

    void performImportExport(ProgressMonitor progressMonitor)
            throws IOException, XmlException, DataAccessException, ActionCancelledException {
        Exporter exporter = new Exporter(getProject().getDesignerDataAccess());

        progressMonitor.start("Exporting Task Templates...");
        progressMonitor.progress(15);

        String xmlString = exporter.exportTaskTemplates(getPackage(), progressMonitor);

        progressMonitor.progress(10);
        progressMonitor.subTask("Writing XML file");
        writeFile(getPage().getFilePath(), xmlString.getBytes());
        progressMonitor.progress(5);
    }
}
