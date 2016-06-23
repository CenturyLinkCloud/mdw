/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.dialogs;

import java.util.List;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class MdwSelectDialog extends TrayDialog
{
  private String message;
  public String getMessage() { return message; }

  private String selection;
  public String getSelection() { return selection; }
  public void setSelection(String selection) { this.selection = selection; }

  private Combo selectCombo;

  private String title = "MDW Select";
  public void setTitle(String title) { this.title = title; }

  private List<String> options;

  public MdwSelectDialog(Shell shell, String message, List<String> options)
  {
    super(shell);
    this.message = message;
    this.options = options;
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    Composite composite = (Composite) super.createDialogArea(parent);
    composite.getShell().setText(title);

    new Label(composite, SWT.NONE).setText(message);

    selectCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
    for (String option : options)
      selectCombo.add(option);

    if (selection != null)
      selectCombo.setText(selection);

    return composite;
  }

  @Override
  protected void cancelPressed()
  {
    setReturnCode(CANCEL);
    close();
  }

  @Override
  protected void okPressed()
  {
    // set the input
    selection = selectCombo.getText();
    if (selection.trim().length() == 0)
      selection = null;
    setReturnCode(OK);
    close();
  }
}