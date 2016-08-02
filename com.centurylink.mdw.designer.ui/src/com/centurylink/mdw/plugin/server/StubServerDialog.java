/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class StubServerDialog extends TrayDialog
{
  public static final int SUBMIT = 0;
  public static final int CANCEL = 1;
  public static final int PASSTHROUGH = 2;
  
  private String masterRequestId;
  private String request;
  
  private String response;
  public String getResponse() { return response; }

  private Text requestMessageText;
  private Text responseMessageText;
  
  public StubServerDialog(Shell shell, String masterRequestId, String request)
  {
    super(shell);
    this.masterRequestId = masterRequestId;
    this.request = request;
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    Composite composite = (Composite) super.createDialogArea(parent);
    composite.getShell().setText("Stub Server Response for " + masterRequestId);
    
    new Label(composite, SWT.NONE).setText("Request Message:");
    requestMessageText = new Text(composite, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
    GridData gd = new GridData(SWT.LEFT);
    gd.widthHint = 500;
    gd.heightHint = 180;
    requestMessageText.setLayoutData(gd);
    requestMessageText.setText(request);
    
    new Label(composite, SWT.NONE).setText("Response Message:");
    responseMessageText = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
    gd = new GridData(SWT.LEFT);
    gd.widthHint = 500;
    gd.heightHint = 180;
    responseMessageText.setLayoutData(gd);
    
    return composite;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent)
  {
    createButton(parent, SUBMIT, "Submit Response", true);
    createButton(parent, PASSTHROUGH, "Passthrough", false);
    createButton(parent, CANCEL, "Cancel", false);
  }

  @Override
  protected void buttonPressed(int buttonId)
  {
    if (buttonId == SUBMIT)
      response = responseMessageText.getText();
    setReturnCode(buttonId);
    close();
  }
}