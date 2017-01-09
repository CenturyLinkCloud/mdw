/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.designer.model.Versionable;

public class SetVersionDialog extends TrayDialog
{
  private Text newVersionTextField;
  private Versionable versionable;

  private int newVersion;
  public int getNewVersion() { return newVersion; }

  private String title;
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public SetVersionDialog(Shell shell, Versionable versionable)
  {
    super(shell);
    this.title = "Set " + versionable.getTitle() + " Version";
    this.versionable = versionable;
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    Composite composite = (Composite) super.createDialogArea(parent);
    composite.getShell().setText(title);

    new Label(composite, SWT.NONE).setText("New Version:");
    newVersionTextField = new Text(composite, SWT.BORDER | SWT.SINGLE);
    GridData gd = new GridData(SWT.LEFT);
    gd.widthHint = 200;
    newVersionTextField.setLayoutData(gd);
    newVersionTextField.setText(versionable.getVersionString());
    newVersionTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        String verStr = newVersionTextField.getText().trim();
        if (verStr.length() == 0)
        {
          getWarningTray().close();
          getButton(IDialogConstants.OK_ID).setEnabled(false);
          return;
        }

        try
        {
          int ver = versionable.parseVersion(verStr);
          if (ver <= versionable.getVersion())
          {
            getButton(IDialogConstants.OK_ID).setEnabled(false);
            WarningTray tray = getWarningTray();
            tray.setMessage("Must be greater than current version: " + versionable.getVersionString());
            tray.open();
          }
          else
          {
            newVersion = ver;
            getWarningTray().close();
            getButton(IDialogConstants.OK_ID).setEnabled(true);
          }
        }
        catch (NumberFormatException ex)
        {
          getButton(IDialogConstants.OK_ID).setEnabled(false);
          WarningTray tray = getWarningTray();
          tray.setMessage("Invalid format: " + ex.getMessage());
          tray.open();
        }
      }
    });

    return composite;
  }

  private WarningTray warningTray;
  public WarningTray getWarningTray()
  {
    if (warningTray == null)
      warningTray = new WarningTray(this);
    return warningTray;
  }
}