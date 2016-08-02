/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import java.io.IOException;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IExportWizard;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.timer.ActionCancelledException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.Exporter;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

public class ExportPackageWizard extends ImportExportWizard implements IExportWizard
{
  ImportExportPage createPage()
  {
    return new ExportPackagePage();
  }

  void performImportExport(ProgressMonitor progressMonitor) throws IOException, XmlException, DataAccessException, ActionCancelledException
  {
    Exporter exporter = new Exporter(getProject().getDesignerDataAccess());
    boolean includeTaskTemplates = false;
    boolean inferReferencedImpls = false;
    IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
    if (getProject().isFilePersist())
      includeTaskTemplates = !prefsStore.getBoolean(PreferenceConstants.PREFS_SUPPRESS_TASK_TEMPLATES_IN_PKG_EXPORT);
    else
      inferReferencedImpls = !prefsStore.getBoolean(PreferenceConstants.PREFS_SUPPRESS_INFER_REFERENCED_IMPLS_DURING_EXPORT);

    List<WorkflowPackage> packages = getPackages();
    String xml;
    if (packages.size() == 1)
      xml = exporter.exportPackage(packages.get(0), includeTaskTemplates, inferReferencedImpls, progressMonitor);
    else
      xml = exporter.exportPackages(packages, includeTaskTemplates, progressMonitor);
    progressMonitor.progress(10);
    progressMonitor.subTask("Writing XML file");
    writeFile(getPage().getFilePath(), xml.getBytes());
    progressMonitor.progress(5);
  }

  protected void postRunUpdates()
  {
    getPackage().fireElementChangeEvent(ChangeType.STATUS_CHANGE, WorkflowPackage.STATUS_EXPORTED);
  }
}
