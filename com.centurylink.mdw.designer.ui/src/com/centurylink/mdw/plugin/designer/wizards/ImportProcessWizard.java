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
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.Importer;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;

public class ImportProcessWizard extends ImportExportWizard {
    ImportExportPage createPage() {
        return new ImportProcessPage();
    }

    void performImportExport(ProgressMonitor progressMonitor) throws IOException, XmlException,
            DataAccessException, ValidationException, ActionCancelledException {
        DesignerProxy designerProxy = getProject().getDesignerProxy();
        Importer importer = new Importer(designerProxy.getPluginDataAccess(), getShell());
        progressMonitor.progress(10);
        progressMonitor.subTask("Reading XML file");
        byte[] bytes = readFile(getPage().getFilePath());
        progressMonitor.progress(20);
        if (progressMonitor.isCanceled())
            throw new ActionCancelledException();
        progressMonitor.subTask("Performing Import");
        WorkflowProcess newProc = importer.importProcess(getPackage(), getProcess(),
                new String(bytes));
        setElement(newProc);
        progressMonitor.progress(30);
        designerProxy.toggleProcessLock(newProc, true);
        designerProxy.savePackage(newProc.getPackage());
    }

    protected void postRunUpdates() {
        WorkflowProcess process = getPage().getProcess();

        // don't know whether process was newly created or updated with a new
        // version -- fire both
        getProject().getDataAccess().getDesignerDataModel().addProcess(process.getProcessVO());
        process.sync();
        process.addElementChangeListener(getProject());
        process.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, process);
        process.fireElementChangeEvent(ChangeType.VERSION_CHANGE, process.getVersionString());
    }
}
