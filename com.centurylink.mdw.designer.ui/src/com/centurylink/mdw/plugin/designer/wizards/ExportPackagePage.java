/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

public class ExportPackagePage extends ImportExportPage
{
  private Button inferImplsCheckbox;

  public ExportPackagePage()
  {
    super("Export MDW Package(s)", "Export XML file for MDW package(s).");
  }

  protected String getDefaultFileName()
  {
    return getPackage().getName() + "-" + getPackage().getVersionString() + ".xml";
  }

  @Override
  protected void createControls(Composite composite, int ncol)
  {
    super.createControls(composite, ncol);
    createInferImplementorsControls(composite, ncol);
  }

  private void createInferImplementorsControls(Composite parent, int ncol)
  {
    inferImplsCheckbox = new Button(parent, SWT.CHECK | SWT.LEFT);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = ncol;
    gd.verticalIndent = 10;
    inferImplsCheckbox.setLayoutData(gd);
    inferImplsCheckbox.setText("Infer Referenced Activity Implementors");
    inferImplsCheckbox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        boolean checked = inferImplsCheckbox.getSelection();
        IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
        prefsStore.setValue(PreferenceConstants.PREFS_SUPPRESS_INFER_REFERENCED_IMPLS_DURING_EXPORT, !checked);
      }
    });
    if (getProject().isFilePersist())
    {
      inferImplsCheckbox.setEnabled(false);
    }
    else
    {
      IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
      boolean inferReferencedImpls = !prefsStore.getBoolean(PreferenceConstants.PREFS_SUPPRESS_INFER_REFERENCED_IMPLS_DURING_EXPORT);
      inferImplsCheckbox.setSelection(inferReferencedImpls);
    }
  }

}
