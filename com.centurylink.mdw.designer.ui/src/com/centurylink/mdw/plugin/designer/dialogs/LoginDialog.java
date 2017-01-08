/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.auth.MdwSecurityException;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class LoginDialog extends Dialog {

  private final static int HORIZONTAL_INDENT = 143;
  private final static int VERTICAL_INDENT = 158;
  private final static int MESSAGE_WIDTH = 250;
  private final static int BUTTON_WIDTH = 80;
  private final static int INPUT_WIDTH = 200;

  private Label messageLabel;
  private Text userNameText;
  private Text passwordText;
  private Button saveCredentialsCheckbox;
  private Button okButton;
  private Button cancelButton;

  private WorkflowProject workflowProject;

  public LoginDialog(Shell parentShell, WorkflowProject project)
  {
    super(parentShell);
    this.workflowProject = project;
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    parent.getShell().setText("MDW Login");
    ImageDescriptor imageDescriptor = MdwPlugin.getImageDescriptor("icons/designer.gif");
    Image iconImage = imageDescriptor.createImage();
    parent.getShell().setImage(iconImage);

    Composite container = new Composite(parent, SWT.NONE);
    container.setBackgroundMode(SWT.INHERIT_DEFAULT);
    imageDescriptor = MdwPlugin.getImageDescriptor("splash.png");
    container.setBackgroundImage(imageDescriptor.createImage());
    container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    GridLayout layout = new GridLayout(3, false);
    container.setLayout(layout);

    GridData gridData = new GridData();
    gridData.minimumWidth = gridData.widthHint = 455;
    gridData.minimumHeight = gridData.heightHint = 295;
    container.setLayoutData(gridData);

    createMessageSection(container);
    createInputSection(container);
    createButtonsSection(container);
    parent.getShell().setDefaultButton(okButton);

    return container;
  }

  private void createMessageSection(Composite parent)
  {
    // message
    messageLabel = new Label(parent, SWT.NONE);
    GridData data = new GridData();
    data.horizontalIndent = HORIZONTAL_INDENT;
    data.verticalIndent = VERTICAL_INDENT;
    data.widthHint = MESSAGE_WIDTH;
    data.horizontalSpan = 3;
    messageLabel.setLayoutData(data);
    Color red = getShell().getDisplay().getSystemColor(SWT.COLOR_RED);
    messageLabel.setForeground(red);
  }

  private void createInputSection(Composite parent)
  {
    // user name
    Label label = new Label(parent, SWT.NONE);
    label.setText("&Username:");
    GridData data = new GridData();
    data.horizontalIndent = HORIZONTAL_INDENT;
    label.setLayoutData(data);
    userNameText = new Text(parent, SWT.BORDER);
    data = new GridData(SWT.NONE, SWT.NONE, false, false);
    data.horizontalSpan = 2;
    data.widthHint = INPUT_WIDTH;
    userNameText.setLayoutData(data);

    // password
    label = new Label(parent, SWT.NONE);
    label.setText("&Password:");
    data = new GridData();
    data.horizontalIndent = HORIZONTAL_INDENT;
    label.setLayoutData(data);
    passwordText = new Text(parent, SWT.PASSWORD | SWT.BORDER);
    data = new GridData(SWT.NONE, SWT.NONE, false, false);
    data.horizontalSpan = 2;
    data.widthHint = INPUT_WIDTH;
    passwordText.setLayoutData(data);

    // spacer
    new Label(parent, SWT.NONE);
    // save credentials
    saveCredentialsCheckbox = new Button(parent, SWT.CHECK | SWT.LEFT);
    saveCredentialsCheckbox.setText("Save in secure store");
    data = new GridData(SWT.NONE, SWT.NONE, false, false);
    data.horizontalSpan = 2;
    saveCredentialsCheckbox.setLayoutData(data);
  }

  private void createButtonsSection(Composite parent)
  {
    // spacer
    new Label(parent, SWT.NONE);

    // ok
    okButton = new Button(parent, SWT.PUSH);
    okButton.setText("OK");
    GridData data = new GridData(SWT.NONE, SWT.NONE, false, false);
    data.widthHint = BUTTON_WIDTH;
    data.verticalIndent = 10;
    okButton.setLayoutData(data);
    okButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable()
        {
          public void run()
          {
            try
            {
              String user = userNameText.getText().trim();
              String password = passwordText.getText().trim();
              workflowProject.authenticate(user, password, saveCredentialsCheckbox.getSelection());
              setReturnCode(OK);
              close();
            }
            catch (MdwSecurityException ex)
            {
              messageLabel.setText(ex.getMessage());
            }
          }
        });
      }
    });

    // cancel
    cancelButton = new Button(parent, SWT.PUSH);
    cancelButton.setText("Cancel");
    data = new GridData(SWT.NONE, SWT.NONE, false, false);
    data.widthHint = BUTTON_WIDTH;
    data.verticalIndent = 10;
    cancelButton.setLayoutData(data);
    cancelButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        setReturnCode(CANCEL);
        close();
      }
    });
  }

  @Override
  protected void createButtonsForButtonBar(final Composite parent)
  {
    GridLayout layout = (GridLayout)parent.getLayout();
    layout.marginHeight = 0;
  }
}