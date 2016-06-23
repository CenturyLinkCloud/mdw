/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.actions.WorkflowElementActionHandler;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.server.ServerConfigurator;
import com.centurylink.mdw.plugin.server.ServerStatusChecker;
import com.centurylink.mdw.plugin.server.ServerStatusListener;

public class ServerConnectionsPropertyPage extends ProjectPropertyPage implements ServerStatusListener
{
  private static final String PARSE_ERROR = "Unable to parse server information";

  private Text containerTextField;
  private Text serverHomeTextField;
  private Button browseServerHomeButton;
  private Text jdkHomeTextField;
  private Button browseJdkHomeButton;
  private Text serverHostTextField;
  private Text serverPortTextField;
  private Text serverCommandPortTextField;
  private Text serverLocTextField;
  private Button browseServerLocButton;
  private Text userTextField;
  private Text passwordTextField;
  private Button newServerButton;
  private Button startServerButton;
  private Label serverStatusLabel;
  private Label serverMessageLabel;
  private Button deployAppButton;
  private Button deleteTempFilesCheckbox;

  private Image serverStartedImage = MdwPlugin.getImageDescriptor("icons/server_started.gif").createImage();
  private Image serverStoppedImage = MdwPlugin.getImageDescriptor("icons/stopped.gif").createImage();
  private Image serverWaitImage = MdwPlugin.getImageDescriptor("icons/wait.gif").createImage();
  private Image serverErrorImage = MdwPlugin.getImageDescriptor("icons/error.gif").createImage();

  private ServerSettings workingCopy;
  private ServerConfigurator configurator;

  private ServerStatusChecker statusCheck;
  private Thread serverStatusThread;

  @Override
  protected Control createContents(Composite parent)
  {
    noDefaultAndApplyButton();
    initializeWorkflowProject();

    Composite composite = createComposite(parent);

    ServerSettings serverSettings = getProject().getServerSettings();
    workingCopy = new ServerSettings(serverSettings);
    configurator = ServerConfigurator.Factory.create(workingCopy);

    createContainerControls(composite);
    addSeparator(composite);
    createServerControls(composite);
    addSeparator(composite);
    createServerStartupControls(composite);
    if (serverSettings.isJavaEE())
    {
      addSeparator(composite);
      createDeployControls(composite);
    }

    if (serverStatusThread != null)
      serverStatusThread.interrupt();
    statusCheck = new ServerStatusChecker(getProject().getServerSettings());
    statusCheck.addStatusListener(this);
    serverStatusThread = new Thread(statusCheck);
    serverStatusThread.start();

    return composite;
  }

  private String getContainerName()
  {
    return workingCopy.getContainerName();
  }

  private void createContainerControls(Composite parent)
  {
    Composite composite = createComposite(parent, 3);

    new Label(composite, SWT.NONE).setText("Container Type:");
    containerTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 150;
    gd.horizontalSpan = 2;
    containerTextField.setLayoutData(gd);
    containerTextField.setEditable(false);
    containerTextField.setText(getContainerName());

    new Label(composite, SWT.NONE).setText(getContainerName() + " Home:");
    serverHomeTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 350;
    serverHomeTextField.setLayoutData(gd);
    serverHomeTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        workingCopy.setHome(serverHomeTextField.getText().trim());
      }
    });
    if (workingCopy.getHome() != null)  // backward compatibility
      serverHomeTextField.setText(workingCopy.getHome());

    browseServerHomeButton = new Button(composite, SWT.PUSH);
    browseServerHomeButton.setText("Browse...");
    browseServerHomeButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        DirectoryDialog dlg = new DirectoryDialog(getShell());
        dlg.setMessage("Select the directory where " + getContainerName() + " is installed.");
        String serverHome = dlg.open();
        if (serverHome != null)
          serverHomeTextField.setText(serverHome);
      }
    });

    new Label(composite, SWT.NONE).setText("Java Home:");
    jdkHomeTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 350;
    jdkHomeTextField.setLayoutData(gd);
    jdkHomeTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        workingCopy.setJdkHome(jdkHomeTextField.getText().trim());
      }
    });
    if (workingCopy.getJdkHome() != null)  // backward compatibility
      jdkHomeTextField.setText(workingCopy.getJdkHome());

    browseJdkHomeButton = new Button(composite, SWT.PUSH);
    browseJdkHomeButton.setText("Browse...");
    browseJdkHomeButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        DirectoryDialog dlg = new DirectoryDialog(getShell());
        dlg.setMessage("Select the directory where Java is installed.");
        String jdkHome = dlg.open();
        if (jdkHome != null)
          jdkHomeTextField.setText(jdkHome);
      }
    });

  }

  private void createServerControls(Composite parent)
  {
    Composite composite = createComposite(parent, 4);

    new Label(composite, SWT.NONE).setText("Server Host:");
    serverHostTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 125;
    gd.horizontalSpan = 3;
    serverHostTextField.setLayoutData(gd);
    serverHostTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        workingCopy.setHost(serverHostTextField.getText().trim());
      }
    });
    serverHostTextField.setText(workingCopy.getHost());

    new Label(composite, SWT.NONE).setText("Server Port:");
    serverPortTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 125;
    gd.horizontalSpan = 3;
    serverPortTextField.setLayoutData(gd);
    serverPortTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        workingCopy.setPort(Integer.parseInt(serverPortTextField.getText().trim()));
      }
    });
    serverPortTextField.setText(Integer.toString(workingCopy.getPort()));

    if (getProject().getServerSettings().isServiceMix() || getProject().getServerSettings().isFuse())
    {
      new Label(composite, SWT.NONE).setText("Server SSH Port:");
      serverCommandPortTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
      gd = new GridData(GridData.BEGINNING);
      gd.widthHint = 125;
      gd.horizontalSpan = 3;
      serverCommandPortTextField.setLayoutData(gd);
      serverCommandPortTextField.addModifyListener(new ModifyListener()
      {
        public void modifyText(ModifyEvent e)
        {
          workingCopy.setCommandPort(Integer.parseInt(serverCommandPortTextField.getText().trim()));
        }
      });
      serverCommandPortTextField.setText(Integer.toString(workingCopy.getCommandPort()));
    }

    if (!getProject().isWar())
    {
      new Label(composite, SWT.NONE).setText(workingCopy.isWebLogic() ? "Domain Location:" : "Server Location:");
      serverLocTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
      gd = new GridData(GridData.BEGINNING);
      gd.widthHint = 350;
      serverLocTextField.setLayoutData(gd);
      serverLocTextField.addModifyListener(new ModifyListener()
      {
        public void modifyText(ModifyEvent e)
        {
          workingCopy.setServerLoc(serverLocTextField.getText().trim());
          try
          {
            configurator.parseServerAdditionalInfo();
            setErrorMessage(null);
            if (serverMessageLabel != null && "Server appears to be running.".equals(serverMessageLabel.getText()))
            {
              if (deployAppButton != null)
                deployAppButton.setEnabled(true);
              if (deleteTempFilesCheckbox != null)
                deleteTempFilesCheckbox.setEnabled(true);
            }
          }
          catch (Exception ex)
          {
            PluginMessages.log(ex);
            setErrorMessage(PARSE_ERROR);
            if (deployAppButton != null)
              deployAppButton.setEnabled(false);
            if (deleteTempFilesCheckbox != null)
              deleteTempFilesCheckbox.setEnabled(false);
            if (startServerButton != null)
              startServerButton.setEnabled(false);
          }
        }
      });
      if (workingCopy.getServerLoc() == null)
      {
        // can happen for old projects not saved with >= 5.1
        setErrorMessage("Please select a server location");
      }
      else
      {
        serverLocTextField.setText(workingCopy.getServerLoc());
      }

      browseServerLocButton = new Button(composite, SWT.PUSH);
      browseServerLocButton.setText("Browse...");
      browseServerLocButton.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          DirectoryDialog dlg = new DirectoryDialog(getShell());
          dlg.setMessage("Select the root directory of your " + getContainerName() + " Server.");
          String serverLoc = dlg.open();
          if (serverLoc != null)
            serverLocTextField.setText(serverLoc);
        }
      });

      newServerButton = new Button(composite, SWT.PUSH);
      newServerButton.setText("New...");
      newServerButton.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          String serverHome = workingCopy.getHome();
          if (serverHome == null || serverHome.trim().length() == 0)
          {
            MessageDialog.openError(getShell(), "Missing Data", "Please enter a value for " + getContainerName() + " Home.");
          }
          else
          {
            String newServerLoc = configurator.launchNewServerCreation(getShell());
            if (newServerLoc != null)
              serverLocTextField.setText(newServerLoc);
          }
        }
      });
    }

    new Label(composite, SWT.NONE).setText(getContainerName() + " User:");
    userTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 125;
    gd.horizontalSpan = 3;
    userTextField.setLayoutData(gd);
    userTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        workingCopy.setUser(userTextField.getText().trim());
      }
    });
    if (workingCopy.getUser() != null)
      userTextField.setText(workingCopy.getUser());

    new Label(composite, SWT.NONE).setText("Password:");
    passwordTextField = new Text(composite, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 125;
    gd.horizontalSpan = 3;
    passwordTextField.setLayoutData(gd);
    passwordTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        workingCopy.setPassword(passwordTextField.getText().trim());
      }
    });
    if (workingCopy.getPassword() != null)  // backward compatibility
      passwordTextField.setText(workingCopy.getPassword());
  }

  private void createServerStartupControls(Composite parent)
  {
    Composite composite = createComposite(parent, 3);

    GridData gd = null;

    if (!workingCopy.isOsgi())
    {
      startServerButton = new Button(composite, SWT.PUSH);
      gd = new GridData(GridData.BEGINNING);
      gd.horizontalSpan = 2;
      startServerButton.setLayoutData(gd);
      startServerButton.setText("Start Server");
      startServerButton.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          performOk(); // save settings to workflowProject
          WorkflowElementActionHandler actionHandler = new WorkflowElementActionHandler();
          actionHandler.run(getProject());
        }
      });
      new Label(composite, SWT.NONE).setText("  (Using MDW Server Runner)");
    }

    serverStatusLabel = new Label(composite, SWT.NONE);
    gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    serverStatusLabel.setLayoutData(gd);
    serverStatusLabel.setVisible(false);
    serverStatusLabel.setImage(serverWaitImage);

    serverMessageLabel = new Label(composite, SWT.NONE);
    gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.heightHint = 20;
    gd.widthHint = 350;
    gd.horizontalSpan = 2;
    serverMessageLabel.setLayoutData(gd);
    serverMessageLabel.setVisible(false);
    serverMessageLabel.setText("Server status has not been determined yet.");
  }

  private void createDeployControls(Composite parent)
  {
    Composite composite = createComposite(parent, 3);

    deployAppButton = new Button(composite, SWT.PUSH);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = 3;
    deployAppButton.setLayoutData(gd);
    if (getProject().isCloudProject())
      deployAppButton.setText("Deploy App");
    else
      deployAppButton.setText("Configure Server");
    deployAppButton.setEnabled(false);
    deployAppButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        performOk(); // save settings to workflowProject
        configurator.doConfigure(getShell());
      }
    });

    Label explainLabel = new Label(composite, SWT.NONE);
    gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = 3;
    explainLabel.setLayoutData(gd);
    String explain = "Configure server resources (JMS Queues, JDBC Connection Pool, etc.)";
    if (getProject().isCloudProject())
      explain += ", then deploy the MDW EAR.";
    explain += "\nServer must be running.";
    explainLabel.setText(explain);

    deleteTempFilesCheckbox = new Button(composite, SWT.CHECK);
    gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = 4;
    gd.horizontalIndent = 5;
    deleteTempFilesCheckbox.setLayoutData(gd);
    deleteTempFilesCheckbox.setText("Delete temporary generated files after configuring");
    deleteTempFilesCheckbox.setSelection(true);
    deleteTempFilesCheckbox.setEnabled(false);
    MdwPlugin.setStringPref(PreferenceConstants.PREFS_DELETE_TEMP_FILES_AFTER_SERVER_CONFIG, Boolean.TRUE.toString());
    deleteTempFilesCheckbox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        MdwPlugin.setStringPref(PreferenceConstants.PREFS_DELETE_TEMP_FILES_AFTER_SERVER_CONFIG, String.valueOf(deleteTempFilesCheckbox.getSelection()));
      }
    });
  }

  public boolean performOk()
  {
    String originalHost = getProject().getServerSettings().getHost();
    int originalPort = getProject().getServerSettings().getPort();

    getProject().setServerSettings(workingCopy);

    IProject project = (IProject) getElement();
    try
    {
      WorkflowProjectManager.getInstance().save(getProject(), project);
    }
    catch (CoreException ex)
    {
      PluginMessages.uiError(getShell(), ex, "Project Settings", getProject());
      return false;
    }

    if (!serverHostTextField.getText().trim().equals(originalHost)
        || Integer.parseInt(serverPortTextField.getText().trim()) != originalPort)
    {
      getProject().fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, workingCopy);
    }

    return true;
  }


  private void updateServerStatus(final String status)
  {
    MdwPlugin.getDisplay().syncExec(new Runnable()
    {
      public void run()
      {
        if (serverStatusLabel.isDisposed() || serverMessageLabel.isDisposed())
          return;

        if (status.equals(SERVER_STATUS_WAIT))
        {
          serverStatusLabel.setImage(serverWaitImage);
          serverMessageLabel.setText("Checking server status...");
          if (deployAppButton != null)
            deployAppButton.setEnabled(false);
          if (deleteTempFilesCheckbox != null)
            deleteTempFilesCheckbox.setEnabled(false);
        }
        else if (status.equals(SERVER_STATUS_RUNNING))
        {
          serverStatusLabel.setImage(serverStartedImage);
          serverMessageLabel.setText("Server appears to be running.");
          if (!PARSE_ERROR.equals(getErrorMessage()) && serverLocTextField != null && serverLocTextField.getText().trim().length() > 0)
          {
            if (startServerButton != null)
              startServerButton.setEnabled(false);
            if (deployAppButton != null)
              deployAppButton.setEnabled(true);
            if (deleteTempFilesCheckbox != null)
              deleteTempFilesCheckbox.setEnabled(true);
            setErrorMessage(null);
          }
        }
        else if (status.equals(SERVER_STATUS_STOPPED))
        {
          serverStatusLabel.setImage(serverStoppedImage);
          serverMessageLabel.setText("Server not reachable at: " + getProject().getServerSettings().getConsoleUrl() + ".");
          if (deployAppButton != null)
            deployAppButton.setEnabled(false);
          if (deleteTempFilesCheckbox != null)
            deleteTempFilesCheckbox.setEnabled(false);
        }
        else if (status.equals(SERVER_STATUS_ERRORED))
        {
          serverStatusLabel.setImage(serverErrorImage);
          serverMessageLabel.setText("Unexpected error accessing the server.  See .metadata/.log for details.");
          if (deployAppButton != null)
            deployAppButton.setEnabled(false);
          if (deleteTempFilesCheckbox != null)
            deleteTempFilesCheckbox.setEnabled(false);
          if (!PARSE_ERROR.equals(getErrorMessage()))
            setErrorMessage("Error accessing the server console");
        }
        serverStatusLabel.setVisible(true);
        serverMessageLabel.setVisible(true);
      }
    });
  }

  @Override
  public void dispose()
  {
    if (serverStatusThread != null)
      serverStatusThread.interrupt();
    super.dispose();
    statusCheck.removeStatusListener(this);
    serverStartedImage.dispose();
    serverStoppedImage.dispose();
    serverWaitImage.dispose();
    serverStatusThread = null;
  }

  public void statusChanged(String newStatus)
  {
    updateServerStatus(newStatus);
  }
}
