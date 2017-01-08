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
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;

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
    WorkflowPackage pkg = getPackage();
    if (pkg == null)
      throw new ValidationException("No package found.");
    importer.importTaskTemplates(pkg, new String(bytes), progressMonitor);
    progressMonitor.progress(30);

  }

  protected void postRunUpdates()
  {
    WorkflowPackage pkg = getPackage();
    if (pkg != null)
    {
      pkg.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, pkg);
      pkg.refreshFolder();
    }
  }
}
