/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import java.io.IOException;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.timer.ActionCancelledException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.designer.utils.ValidationException;
import com.centurylink.mdw.plugin.designer.Importer;

public class ImportTaskTemplatesWizard extends ImportExportWizard
{
  ImportExportPage createPage()
  {
    return new ImportTaskTemplatesPage();
  }

  void performImportExport(ProgressMonitor progressMonitor) throws IOException, XmlException, DataAccessException, ValidationException, ActionCancelledException
  {
    Importer importer = new Importer(getProject().getDataAccess(), getShell());
    progressMonitor.progress(10);
    progressMonitor.subTask("Reading XML file");
    byte[] bytes = readFile(getPage().getFilePath());
    if (progressMonitor.isCanceled())
      throw new ActionCancelledException();
    progressMonitor.subTask("Performing Import");
    importer.importTaskTemplates(getProject(), new String(bytes), progressMonitor);
    progressMonitor.progress(30);
  }

  protected void postRunUpdates()
  {
    // TODO: refresh package UI
  }
}
