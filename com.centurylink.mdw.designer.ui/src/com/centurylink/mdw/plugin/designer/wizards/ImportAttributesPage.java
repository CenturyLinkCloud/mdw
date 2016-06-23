/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class ImportAttributesPage extends ImportExportPage
{
  private Label prefixLabel;

  public ImportAttributesPage()
  {
    super("Import MDW Attributes", "Import override attributes from an XML file.");
  }

  @Override
  protected void createControls(Composite composite, int ncol)
  {
    createProjectControls(composite, ncol);
    if (getProcess() == null)
      createPackageControls(composite, ncol);
    else
      createProcessControls(composite, ncol);
    createPrefixControls(composite, ncol);
    createFileControls(composite, ncol);
  }

  private void createPrefixControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Attribute Type:");
    prefixLabel = new Label(parent, SWT.NONE);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol - 1;
    prefixLabel.setLayoutData(gd);
    FontData font = prefixLabel.getFont().getFontData()[0];
    font.setStyle(font.getStyle() | SWT.BOLD);
    prefixLabel.setFont(new Font(this.getShell().getDisplay(), font));
    prefixLabel.setText(getPrefix());
  }

  private String getPrefix()
  {
    return ((ImportAttributesWizard)getWizard()).getPrefix();
  }
}