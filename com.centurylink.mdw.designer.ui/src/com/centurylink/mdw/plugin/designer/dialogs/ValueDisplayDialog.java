/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ValueDisplayDialog extends TrayDialog
{
  private String title;
  private String value;
  private boolean createCancelButton;

  public ValueDisplayDialog(Shell shell, String value)
  {
    this(shell, value, "Value", false);
  }
  
  public ValueDisplayDialog(Shell shell, String title, String value, boolean cancelButton)
  {
    super(shell);
    this.title = title;
    this.value = value;
    this.createCancelButton = cancelButton;
  }
  
  @Override
  protected Control createDialogArea(Composite parent)
  {
    Composite composite = (Composite) super.createDialogArea(parent);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    composite.setLayout(layout);
    composite.getShell().setText(title);
    
    // value
    Text valueText = new Text(composite, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 500;
    gd.heightHint = 400;
    valueText.setLayoutData(gd);
    if (value != null)
      valueText.setText(value);

    return composite;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent)
  {
    Button okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    okButton.forceFocus();
    
    if (createCancelButton)
      createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

}