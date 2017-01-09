/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.actions.WorkflowElementActionHandler;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.service.ApplicationSummaryDocument.ApplicationSummary;

public class ProcessLaunchMainTab extends AbstractLaunchConfigurationTab
{
  private WorkflowProject project;
  public WorkflowProject getProject() { return project; }
  private WorkflowProcess process;
  public WorkflowProcess getProcess() { return process; }

  private Combo workflowProjectCombo;
  private Combo processNameCombo;
  protected Combo processVersionCombo;
  private Text masterRequestIdText;
  private Text ownerText;
  private Text ownerIdText;
  private Label serverStatusLabel;
  private Label serverMessageLabel;
  private Button execModeSyncButton;
  private Button execModeAsyncButton;
  private Combo responseVarNameCombo;
  private Button showLogOutputCheckbox;
  private Label logWatcherPortLabel;
  private Text logWatcherPortText;
  private Button liveViewCheckbox;

  private Image image = MdwPlugin.getImageDescriptor("icons/launch.gif").createImage();
  private Image serverStartedImage = MdwPlugin.getImageDescriptor("icons/server_started.gif").createImage();
  private Image serverStoppedImage = MdwPlugin.getImageDescriptor("icons/stopped.gif").createImage();
  private Image serverWaitImage = MdwPlugin.getImageDescriptor("icons/wait.gif").createImage();
  private Image serverErrorImage = MdwPlugin.getImageDescriptor("icons/error.gif").createImage();
  private Image liveViewImage = MdwPlugin.getImageDescriptor("icons/display_prefs.gif").createImage();

  private Thread serverStatusThread;
  private static final String SERVER_STATUS_RUNNING = "running";
  private static final String SERVER_STATUS_STOPPED = "stopped";
  private static final String SERVER_STATUS_WAIT = "wait";

  public void createControl(Composite parent)
  {
    Composite composite = new Composite(parent, SWT.NONE);
    setControl(composite);

    GridLayout topLayout = new GridLayout();
    topLayout.numColumns = 2;
    composite.setLayout(topLayout);

    createWorkflowProjectSection(composite);
    createProcessSection(composite);
    createInstanceInfoSection(composite);
    createExecutionModeSection(composite);
    createServerStatusSection(composite);
    createLiveViewSection(composite);
  }

  public String getName()
  {
    return "Process";
  }

  public Image getImage()
  {
    return image;
  }

  public void initializeFrom(ILaunchConfiguration launchConfig)
  {
    try
    {
      String wfProject = launchConfig.getAttribute(ProcessLaunchConfiguration.WORKFLOW_PROJECT, "");
      project = WorkflowProjectManager.getInstance().getWorkflowProject(wfProject);
      if (project == null)  // may no longer be in workspace
        return ;

      workflowProjectCombo.setText(project.getName());
      refreshProcesses();

      if (serverStatusThread != null)
        serverStatusThread.interrupt();
      serverStatusThread = new Thread(new ServerStatusChecker());
      serverStatusThread.start();
      String procName = launchConfig.getAttribute(ProcessLaunchConfiguration.PROCESS_NAME, "");
      if (procName.length() > 0)
      {
        processNameCombo.setText(procName);
        refreshVersions();
      }
      String procVer = launchConfig.getAttribute(ProcessLaunchConfiguration.PROCESS_VERSION, "");
      if (procVer.length() > 0)
      {
        processVersionCombo.setText(procVer);
        ProcessVO processVO = project.getDesignerProxy().getProcessVO(processNameCombo.getText(), procVer);
        if (project != null && processVO != null)
        {
          process = new WorkflowProcess(project, processVO);
          openProcess(process);
          refreshVariables(process);
        }
      }
      masterRequestIdText.setText(getMasterRequestId());
      ownerText.setText(getOwner());
      ownerIdText.setText(getOwnerId().toString());

      if (execModeAsyncButton != null)
      {
        boolean synchronous = launchConfig.getAttribute(ProcessLaunchConfiguration.SYNCHRONOUS, false);
        execModeAsyncButton.setSelection(!synchronous);
        execModeSyncButton.setSelection(synchronous);

        String responseVarName = launchConfig.getAttribute(ProcessLaunchConfiguration.RESPONSE_VAR_NAME, "");
        responseVarNameCombo.setText(responseVarName);
        responseVarNameCombo.setEnabled(synchronous);
      }

      boolean showLogs = launchConfig.getAttribute(ProcessLaunchConfiguration.SHOW_LOGS, false);
      showLogOutputCheckbox.setSelection(showLogs);

      int logWatcherPort = launchConfig.getAttribute(ProcessLaunchConfiguration.LOG_WATCHER_PORT, project.getServerSettings().getLogWatcherPort());
      logWatcherPortText.setText(String.valueOf(logWatcherPort));

      if (showLogs)
      {
        boolean liveView = launchConfig.getAttribute(ProcessLaunchConfiguration.LIVE_VIEW, false);
        liveViewCheckbox.setSelection(liveView);
      }
      else
      {
        logWatcherPortLabel.setEnabled(false);
        logWatcherPortText.setEnabled(false);
        liveViewCheckbox.setEnabled(false);
      }

      enableLiveView(!project.isRemote());
    }
    catch (CoreException ex)
    {
      PluginMessages.uiError(ex, "Launch Init", project);
    }
    validatePage();
  }

  public void performApply(ILaunchConfigurationWorkingCopy launchConfig)
  {
    launchConfig.setAttribute(ProcessLaunchConfiguration.WORKFLOW_PROJECT, workflowProjectCombo.getText());
    launchConfig.setAttribute(ProcessLaunchConfiguration.PROCESS_NAME, processNameCombo.getText());
    launchConfig.setAttribute(ProcessLaunchConfiguration.PROCESS_VERSION, processVersionCombo.getText());
    launchConfig.setAttribute(ProcessLaunchConfiguration.MASTER_REQUEST_ID, masterRequestIdText.getText().trim());
    launchConfig.setAttribute(ProcessLaunchConfiguration.OWNER, ownerText.getText());
    launchConfig.setAttribute(ProcessLaunchConfiguration.OWNER_ID, ownerIdText.getText());
    if (execModeSyncButton != null)
    {
      launchConfig.setAttribute(ProcessLaunchConfiguration.SYNCHRONOUS, execModeSyncButton.getSelection());
      launchConfig.setAttribute(ProcessLaunchConfiguration.RESPONSE_VAR_NAME, responseVarNameCombo.getText());
    }
    launchConfig.setAttribute(ProcessLaunchConfiguration.SHOW_LOGS, showLogOutputCheckbox.getSelection());
    try
    {
      int logWatchPort = Integer.parseInt(logWatcherPortText.getText());
      launchConfig.setAttribute(ProcessLaunchConfiguration.LOG_WATCHER_PORT, logWatchPort);
    }
    catch (NumberFormatException ex)
    {
      // don't set port
    }
    launchConfig.setAttribute(ProcessLaunchConfiguration.LIVE_VIEW, liveViewCheckbox.getSelection());
  }

  public void setDefaults(ILaunchConfigurationWorkingCopy launchConfig)
  {
    launchConfig.setAttribute(ProcessLaunchConfiguration.MASTER_REQUEST_ID, getMasterRequestId());
    launchConfig.setAttribute(ProcessLaunchConfiguration.OWNER, getOwner());
    launchConfig.setAttribute(ProcessLaunchConfiguration.OWNER_ID, getOwnerId().toString());
  }

  protected void createWorkflowProjectSection(Composite parent)
  {
    List<WorkflowProject> workflowProjects = WorkflowProjectManager.getInstance().getWorkflowProjects();
    if (workflowProjects == null || workflowProjects.size() == 0)
      MessageDialog.openError(parent.getShell(), "Error", "No MDW Workflow projects found");

    new Label(parent, SWT.NONE).setText("Workflow Project:");
    workflowProjectCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 200;
    gd.verticalIndent = 3;
    workflowProjectCombo.setLayoutData(gd);

    workflowProjectCombo.removeAll();
    for (WorkflowProject project : workflowProjects)
    {
      workflowProjectCombo.add(project.getName());
    }

    workflowProjectCombo.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        project = WorkflowProjectManager.getInstance().getWorkflowProject(workflowProjectCombo.getText());
        process = null;
        refreshProcesses();
        processNameCombo.select(0);
        refreshVersions();
        processVersionCombo.select(0);
        enableLiveView(!project.isRemote());
        setDirty(true);
        validatePage();

        if (project != null)
        {
          if (serverStatusThread != null)
            serverStatusThread.interrupt();
          serverStatusThread = new Thread(new ServerStatusChecker());
          serverStatusThread.start();
        }
      }
    });
  }

  protected void createProcessSection(Composite parent)
  {
    Group processVersionGroup = new Group(parent, SWT.NONE);
    processVersionGroup.setText("Process Version");
    GridLayout gl = new GridLayout();
    gl.numColumns = 5;
    processVersionGroup.setLayout(gl);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = 2;
    gd.grabExcessHorizontalSpace = true;
    processVersionGroup.setLayoutData(gd);

    new Label(processVersionGroup, SWT.NONE).setText("Process: ");

    processNameCombo = new Combo(processVersionGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 250;
    processNameCombo.setLayoutData(gd);
    refreshProcesses();
    processNameCombo.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        refreshVersions();
        setDirty(true);
        validatePage();
      }
    });

    new Label(processVersionGroup, SWT.NONE).setText("    "); // spacer

    new Label(processVersionGroup, SWT.NONE).setText("Version: ");

    processVersionCombo = new Combo(processVersionGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 35;
    processVersionCombo.setLayoutData(gd);
    refreshVersions();
    processVersionCombo.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        String version = processVersionCombo.getText();
        if (version.length() == 0)
        {
          process = null;
        }
        else
        {
          String name = processNameCombo.getText().trim();
          if (name.length() == 0)
          {
            process = null;
          }
          else
          {
            ProcessVO processVO = project.getDesignerProxy().getProcessVO(name, version);
            if (processVO != null)
            {
              process = new WorkflowProcess(project, processVO);
              openProcess(process);
              refreshVariables(process);
            }
          }
        }
        setDirty(true);
        validatePage();
      }
    });
  }

  protected void createInstanceInfoSection(Composite parent)
  {
    Group instanceInfoGroup = new Group(parent, SWT.NONE);
    instanceInfoGroup.setText("Instance Info");
    GridLayout gl = new GridLayout();
    gl.numColumns = 5;
    instanceInfoGroup.setLayout(gl);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = 2;
    instanceInfoGroup.setLayoutData(gd);

    new Label(instanceInfoGroup, SWT.NONE).setText("Master Request ID: ");
    masterRequestIdText = new Text(instanceInfoGroup, SWT.BORDER | SWT.SINGLE);
    gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.widthHint = 345;
    gd.horizontalSpan = 4;
    masterRequestIdText.setLayoutData(gd);
    if (project != null)
      masterRequestIdText.setText(getMasterRequestId());

    masterRequestIdText.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        setDirty(true);
        validatePage();
      }
    });

    new Label(instanceInfoGroup, SWT.NONE).setText("Owner: ");
    ownerText = new Text(instanceInfoGroup, SWT.BORDER | SWT.SINGLE | SWT.READ_ONLY);
    gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.widthHint = 150;
    ownerText.setLayoutData(gd);
    if (project != null)
      ownerText.setText(getOwner());


    new Label(instanceInfoGroup, SWT.NONE).setText("    "); // spacer

    new Label(instanceInfoGroup, SWT.NONE).setText("Owner ID: ");
    ownerIdText = new Text(instanceInfoGroup, SWT.BORDER | SWT.SINGLE | SWT.READ_ONLY);
    gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.widthHint = 150;
    ownerIdText.setLayoutData(gd);
    if (project != null)
      ownerIdText.setText(getOwnerId().toString());
  }

  protected void createExecutionModeSection(Composite parent)
  {
    Group execModeGroup = new Group(parent, SWT.NONE);
    execModeGroup.setText("Execution Mode");
    GridLayout gl = new GridLayout();
    gl.numColumns = 4;
    execModeGroup.setLayout(gl);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = 2;
    execModeGroup.setLayoutData(gd);

    execModeAsyncButton = new Button(execModeGroup, SWT.RADIO | SWT.LEFT);
    execModeAsyncButton.setText("Asynchronous");
    execModeAsyncButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        if (!execModeSyncButton.getSelection())
          responseVarNameCombo.setText("");
        responseVarNameCombo.setEnabled(execModeSyncButton.getSelection());
        setDirty(true);
        validatePage();
      }
    });

    new Label(execModeGroup, SWT.NONE).setText("    "); // spacer

    execModeSyncButton = new Button(execModeGroup, SWT.RADIO | SWT.LEFT);
    execModeSyncButton.setText("Synchronous");
    execModeSyncButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        if (!execModeSyncButton.getSelection())
          responseVarNameCombo.setText("");
        responseVarNameCombo.setEnabled(execModeSyncButton.getSelection());
        setDirty(true);
        validatePage();
      }
    });

    new Label(execModeGroup, SWT.NONE);
    new Label(execModeGroup, SWT.NONE);
    new Label(execModeGroup, SWT.NONE);
    new Label(execModeGroup, SWT.NONE).setText("  Response Variable: ");
    responseVarNameCombo = new Combo(execModeGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 260;
    responseVarNameCombo.setLayoutData(gd);
    responseVarNameCombo.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        setDirty(true);
        validatePage();
      }
    });
  }

  protected void createServerStatusSection(Composite parent)
  {
    Group serverStatusGroup = new Group(parent, SWT.NONE);
    serverStatusGroup.setText("Server Status");
    GridLayout gl = new GridLayout();
    gl.numColumns = 5;
    serverStatusGroup.setLayout(gl);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = 2;
    serverStatusGroup.setLayoutData(gd);

    serverStatusLabel = new Label(serverStatusGroup, SWT.NONE);
    gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    serverStatusLabel.setLayoutData(gd);
    serverStatusLabel.setVisible(false);
    serverStatusLabel.setImage(serverWaitImage);

    serverMessageLabel = new Label(serverStatusGroup, SWT.NONE);
    gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.heightHint = 45;
    gd.widthHint = 450;
    serverMessageLabel.setLayoutData(gd);
    serverMessageLabel.setVisible(false);
    serverMessageLabel.setText("Server status has not been determined yet.");
  }

  protected void createLiveViewSection(Composite parent)
  {
    Group liveViewGroup = new Group(parent, SWT.NONE);
    liveViewGroup.setText("Live View");
    GridLayout gl = new GridLayout();
    gl.numColumns = 3;
    liveViewGroup.setLayout(gl);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = 2;
    liveViewGroup.setLayoutData(gd);

    showLogOutputCheckbox = new Button(liveViewGroup, SWT.CHECK);
    showLogOutputCheckbox.setText("Monitor Runtime Log");
    gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = 3;
    showLogOutputCheckbox.setLayoutData(gd);
    showLogOutputCheckbox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        if (showLogOutputCheckbox.getSelection())
        {
          logWatcherPortLabel.setEnabled(true);
          logWatcherPortText.setEnabled(true);
          liveViewCheckbox.setEnabled(true);
        }
        else
        {
          logWatcherPortLabel.setEnabled(false);
          logWatcherPortText.setEnabled(false);
          liveViewCheckbox.setSelection(false);
          liveViewCheckbox.setEnabled(false);
        }
        setDirty(true);
        validatePage();
      }
    });

    logWatcherPortLabel = new Label(liveViewGroup, SWT.NONE);
    logWatcherPortLabel.setText("Log Watcher Port: ");
    gd = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalIndent = 20;
    logWatcherPortLabel.setLayoutData(gd);

    logWatcherPortText = new Text(liveViewGroup, SWT.BORDER | SWT.SINGLE);
    gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.widthHint = 65;
    logWatcherPortText.setLayoutData(gd);
    logWatcherPortText.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        setDirty(true);
        validatePage();
      }
    });

    liveViewCheckbox = new Button(liveViewGroup, SWT.CHECK);
    liveViewCheckbox.setText("Process Instance Live View");
    liveViewCheckbox.setImage(liveViewImage);
    gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalIndent = 20;
    liveViewCheckbox.setLayoutData(gd);
    liveViewCheckbox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        setDirty(true);
        validatePage();
      }
    });
  }

  private void enableLiveView(boolean enabled)
  {
    if (!enabled)
      showLogOutputCheckbox.setSelection(false);
    showLogOutputCheckbox.setEnabled(enabled);
  }

  class ServerStatusChecker implements Runnable
  {
    public void run()
    {
      updateServerStatus(SERVER_STATUS_WAIT, null);
      while (true)
      {
        try
        {
          DesignerProxy designerProxy = project.getDesignerProxy();
          ApplicationSummary appSummary = designerProxy.retrieveAppSummary(true);
          if (appSummary == null)
          {
            updateServerStatus(SERVER_STATUS_STOPPED, null);
          }
          else
          {
            String warn = null;
            if (!getProject().isFilePersist())
              warn = designerProxy.checkForDbMismatch(appSummary);
            updateServerStatus(SERVER_STATUS_RUNNING, warn);
          }
          Thread.sleep(6000);
        }
        catch (InterruptedException ex)
        {
          break;
        }
      }
    }
  }

  private void updateServerStatus(final String status, final String msg)
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
        }
        else if (status.equals(SERVER_STATUS_RUNNING))
        {
          if (msg == null)
          {
            serverStatusLabel.setImage(serverStartedImage);
            serverMessageLabel.setText("Server appears to be running.");
            setErrorMessage(null);
          }
          else
          {
            if (!getProject().isFilePersist())
            {
              serverStatusLabel.setImage(serverErrorImage);
              serverMessageLabel.setText("Server DB mismatch:\n" + msg);
              setErrorMessage("Please ensure that the server database configuration matches the workflow project.");
            }
          }
        }
        else if (status.equals(SERVER_STATUS_STOPPED))
        {
          serverStatusLabel.setImage(serverStoppedImage);
          serverMessageLabel.setText("Server appears to be stopped.");
          String msg = "Please ensure that the server is running and reachable";
          if (project == null)
          {
            msg += ".";
          }
          else
          {
            ServerSettings serverSettings = project.getServerSettings();
            msg += "  on " + serverSettings.getHost() + ":" + serverSettings.getPort() + ".";
          }
          setErrorMessage(msg);
        }
        serverStatusLabel.setVisible(true);
        serverMessageLabel.setVisible(true);
        updateLaunchConfigurationDialog();
      }
    });
  }

  private String getMasterRequestId()
  {
    if (project == null)
      return "";

    SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd-HHmmss");
    return getProject().getUser().getUsername() + "~" + sdf.format(new Date());
  }

  private String getOwner()
  {
    return "Designer";
  }

  private Long getOwnerId()
  {
    return new Date().getTime();
  }

  protected void validatePage()
  {
    setErrorMessage(null);
    setMessage(null);

    if (project == null)
    {
      setErrorMessage("Please select a Workflow Project");
      updateLaunchConfigurationDialog();
      return;
    }
    if (process == null)
    {
      setErrorMessage("Please select a Workflow Process to launch");
      updateLaunchConfigurationDialog();
      return;
    }
    if (masterRequestIdText.getText().trim().length() == 0)
    {
      setErrorMessage("Please enter a value for Master Request ID");
      updateLaunchConfigurationDialog();
      return;
    }
    if (showLogOutputCheckbox.getSelection())
    {
      try
      {
        Integer.parseInt(logWatcherPortText.getText());
      }
      catch (NumberFormatException ex)
      {
        setErrorMessage("Invalid Log Watcher port number");
      }
    }

    updateLaunchConfigurationDialog();
  }

  @Override
  public boolean canSave()
  {
    return getErrorMessage() == null;
  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig)
  {
    return canSave();
  }

  private void refreshProcesses()
  {
    BusyIndicator.showWhile(getControl().getShell().getDisplay(), new Runnable()
    {
      public void run()
      {
        processNameCombo.removeAll();
        processNameCombo.add("");
        if (project != null)
        {
          List<ProcessVO> processVOs = project.getDataAccess().getProcesses(false);
          for (ProcessVO processVO : processVOs)
            processNameCombo.add(processVO.getProcessName());
          if (process != null)
            processNameCombo.setText(process.getName());
        }
      }
    });
  }

  private void refreshVersions()
  {
    processVersionCombo.removeAll();
    processVersionCombo.add("");

    String processName = processNameCombo.getText();
    if (processName.length() == 0)
    {
      process = null;
      processVersionCombo.select(0);
    }
    else
    {
      ProcessVO processVO = project.getDesignerProxy().getLatestProcessVO(processName);
      process = new WorkflowProcess(project, processVO);
      for (WorkflowProcess pv : process.getAllProcessVersions())
      {
        processVersionCombo.add(pv.getVersionString());
      }
      processVersionCombo.select(1);
    }
  }

  private void refreshVariables(WorkflowProcess processVersion)
  {
    if (responseVarNameCombo != null)
    {
      String responseVar = responseVarNameCombo.getText();
      responseVarNameCombo.removeAll();
      responseVarNameCombo.add("");
      for (VariableVO outputVar : processVersion.getOutputVariables())
        responseVarNameCombo.add(outputVar.getVariableName());

      responseVarNameCombo.setText(responseVar);
    }
  }

  public void dispose()
  {

    if (serverStatusThread != null)
      serverStatusThread.interrupt();
    super.dispose();
    image.dispose();
    serverStartedImage.dispose();
    serverStoppedImage.dispose();
    serverWaitImage.dispose();
    serverStatusThread = null;
  }

  private void openProcess(WorkflowProcess processVersion)
  {
    WorkflowElementActionHandler actionHandler = new WorkflowElementActionHandler();
    actionHandler.open(processVersion);
  }
}
