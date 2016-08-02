/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.dialogs;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;

public class PackageDeleteDialog extends TrayDialog
{
  private WorkflowPackage packageToDelete;

  public PackageDeleteDialog(Shell shell, WorkflowPackage packageToDelete)
  {
    super(shell);
    this.packageToDelete = packageToDelete;
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    Composite composite = (Composite) super.createDialogArea(parent);
    composite.getShell().setText("Delete Package");
    new Label(composite, SWT.NONE).setText("Delete package '" + packageToDelete.getName() + "' v" + packageToDelete.getVersionString() + "?");
    if (packageToDelete.getProject().isFilePersist())
      new Label(composite, SWT.NONE).setText("All workflow elements within this package will be deleted.");
    else
      new Label(composite, SWT.NONE).setText("Workflow elements within this package will revert to the default package and will have to be deleted separately.");
    return composite;
  }
}
