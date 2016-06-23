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
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;

public class ExportAssetWizard extends ImportExportWizard implements IExportWizard
{
  ImportExportPage createPage()
  {
    return new ExportAssetPage();
  }

  void performImportExport(ProgressMonitor progressMonitor) throws IOException, XmlException, DataAccessException, ActionCancelledException
  {
    WorkflowAsset asset = getAsset();
    if (!asset.isLoaded())
      asset.load();
    progressMonitor.progress(20);
    byte[] bytes = asset.isBinary() ? asset.getDecodedContent() : asset.getContent().getBytes();
    writeFile(getPage().getFilePath(), bytes);
    progressMonitor.progress(5);
  }
}
