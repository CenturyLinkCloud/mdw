/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import java.io.IOException;

import org.apache.xmlbeans.XmlException;
import org.eclipse.ui.IExportWizard;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.timer.ActionCancelledException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.plugin.designer.Exporter;

public class ExportTaskTemplatesWizard extends ImportExportWizard implements IExportWizard
{
  ImportExportPage createPage()
  {
    return new ExportTaskTemplatesPage();
  }

  void performImportExport(ProgressMonitor progressMonitor) throws IOException, XmlException, DataAccessException, ActionCancelledException
  {
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
