/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import java.io.IOException;

import org.apache.xmlbeans.XmlException;
import org.eclipse.ui.IExportWizard;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.plugin.designer.Exporter;

public class ExportProcessWizard extends ImportExportWizard implements IExportWizard {
    ImportExportPage createPage() {
        return new ExportProcessPage();
    }

    void performImportExport(ProgressMonitor progressMonitor)
            throws IOException, XmlException, DataAccessException {
        Exporter exporter = new Exporter(getProject().getDesignerDataAccess());
        String xmlString = exporter.exportProcess(getProcess().getName(),
                getProcess().getVersionString(), false);
        progressMonitor.progress(10);
        progressMonitor.subTask("Writing XML file");
        String fileName = getPage().getFilePath();
        writeFile(fileName, xmlString.getBytes());
        progressMonitor.progress(5);
    }
}
