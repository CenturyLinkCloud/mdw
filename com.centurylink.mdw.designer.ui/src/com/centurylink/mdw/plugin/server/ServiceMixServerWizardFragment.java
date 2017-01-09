/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.centurylink.mdw.plugin.MdwPlugin;

public class ServiceMixServerWizardFragment extends WizardFragment
{
  private Text serverLocTextField;
  private Button browseServerLocButton;
  private Text serverPortTextField;
  private Text sshPortTextField;
  private Text userTextField;
  private Text passwordTextField;

  private ServiceMixServer server;
  private Composite composite;

  @Override
  public boolean hasComposite()
  {
    return true;
  }

  @Override
  public Composite createComposite(Composite parent, final IWizardHandle wizard)
  {
    composite = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    composite.setLayout(layout);

    server = loadServer();
    if (server == null)
        return composite;

    wizard.setTitle(server.getRuntime().getRuntimeType().getName() + " Server");
    wizard.setDescription("Specify your " + server.getName() + " instance settings");
    wizard.setImageDescriptor(MdwPlugin.getImageDescriptor("icons/server_wiz.png"));

    GridData data = new GridData();
    data.verticalAlignment = GridData.FILL;
    data.horizontalAlignment = GridData.FILL;
    composite.setLayoutData(data);

    new Label(composite, SWT.NONE).setText("Location:");
    serverLocTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 350;
    serverLocTextField.setLayoutData(gd);
    if (server.getLocation() != null)
      serverLocTextField.setText(server.getLocation());
    serverLocTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        server.setLocation(serverLocTextField.getText().trim());
        if (server.validateServerLoc() == null)
        {
          if (userTextField.getText().trim().isEmpty())
          {
            IPath runtimeLoc = server.getRuntime().getLocation();
            boolean isInstance = true;
            if (runtimeLoc == null) // old runtimes had no location
              isInstance = server.getLocation().indexOf("instances") > 0;
            else
              isInstance = !new File(server.getLocation()).toString().equals(runtimeLoc.toFile().toString());
            if (isInstance)
              userTextField.setText("karaf");
            else
              userTextField.setText("smx");
          }
          if (sshPortTextField.getText().trim().isEmpty())
          {
            String sshPort = server.readSshPortProp();
            if (sshPort == null)
              sshPort = "8101";
            sshPortTextField.setText(sshPort);
          }
        }
        validate(wizard);
      }
    });

    browseServerLocButton = new Button(composite, SWT.PUSH);
    browseServerLocButton.setText("Browse...");
    browseServerLocButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        DirectoryDialog dlg = new DirectoryDialog(browseServerLocButton.getShell());
        dlg.setMessage("Select your " + server.getName() + " server directory (root or instance)");
        String serverLoc = dlg.open();
        if (serverLoc != null)
          serverLocTextField.setText(serverLoc);
      }
    });

    new Label(composite, SWT.NONE).setText("HTTP Port:");
    serverPortTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 125;
    gd.horizontalSpan = 2;
    serverPortTextField.setLayoutData(gd);
    if (server.getPort() > 0)
    {
      serverPortTextField.setText(Integer.toString(server.getPort()));
      server.setPort(server.getPort());
    }
    serverPortTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        try
        {
          server.setPort(Integer.parseInt(serverPortTextField.getText().trim()));
        }
        catch (NumberFormatException ex)
        {
          server.setPort(0);
        }
        validate(wizard);
      }
    });

    new Label(composite, SWT.NONE).setText("SSH Port:");
    sshPortTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 125;
    gd.horizontalSpan = 2;
    sshPortTextField.setLayoutData(gd);
    if (server.getSshPort() > 0)
      sshPortTextField.setText(Integer.toString(server.getSshPort()));
    sshPortTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        try
        {
          server.setSshPort(Integer.parseInt(sshPortTextField.getText().trim()));
        }
        catch (NumberFormatException ex)
        {
          server.setSshPort(0);
        }
        validate(wizard);
      }
    });

    new Label(composite, SWT.NONE).setText("Karaf User:");
    userTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 125;
    gd.horizontalSpan = 2;
    userTextField.setLayoutData(gd);
    if (server.getUser() != null)
      userTextField.setText(server.getUser());
    userTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        server.setUser(userTextField.getText().trim());
        validate(wizard);
      }
    });

    new Label(composite, SWT.NONE).setText("Password:");
    passwordTextField = new Text(composite, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 125;
    gd.horizontalSpan = 2;
    passwordTextField.setLayoutData(gd);
    if (server.getPassword() != null)
      passwordTextField.setText(server.getPassword());
    passwordTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        server.setPassword(passwordTextField.getText().trim());
        validate(wizard);
      }
    });

    return composite;
  }

  protected ServiceMixServer loadServer()
  {
    IServerWorkingCopy serverWC = (IServerWorkingCopy)getTaskModel().getObject(TaskModel.TASK_SERVER);
    return (ServiceMixServer)serverWC.loadAdapter(ServiceMixServer.class, null);
  }

  @Override
  public boolean isComplete()
  {
    if (server == null)
      return false;

    IStatus status = server.validate();
    return (status == null || status.getSeverity() != IStatus.ERROR);
  }

  protected boolean validate(IWizardHandle wizard)
  {
    if (server == null)
    {
      wizard.setMessage("", IMessageProvider.ERROR);
      return false;
    }

    IStatus status = server.validate();
    if (status == null || status.isOK())
      wizard.setMessage(null, IMessageProvider.NONE);
    else if (status.getSeverity() == IStatus.WARNING)
      wizard.setMessage(status.getMessage(), IMessageProvider.WARNING);
    else
      wizard.setMessage(status.getMessage(), IMessageProvider.ERROR);
    wizard.update();

    return status == null || status.isOK();
  }
}
