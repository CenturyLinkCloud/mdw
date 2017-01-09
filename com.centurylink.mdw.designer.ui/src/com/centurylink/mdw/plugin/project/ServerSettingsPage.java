/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jst.j2ee.project.facet.EARFacetProjectCreationDataModelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetProjectCreationDataModelProperties;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.RuntimeManager;
import org.eclipse.wst.common.project.facet.ui.IFacetWizardPage;
import org.eclipse.wst.common.project.facet.ui.IWizardContext;
import org.eclipse.wst.common.project.facet.ui.ModifyFacetedProjectWizard;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.web.ui.internal.wizards.DataModelFacetCreationWizardPage;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.ServerSettings.ContainerType;
import com.centurylink.mdw.plugin.project.model.WorkflowProject.PersistType;
import com.centurylink.mdw.plugin.server.ServerConfigurator;

@SuppressWarnings("deprecation")
public abstract class ServerSettingsPage extends WizardPage implements IFacetWizardPage
{
  private Text serverHomeTextField;
  private Button browseServerHomeButton;
  private Combo targetRuntimeCombo;
  private Button newRuntimeButton;
  private Text jdkHomeTextField;
  private Button browseJdkHomeButton;
  protected Text serverHostTextField;
  protected Text serverPortTextField;
  protected Text serverLocTextField;
  private Button browseServerLocButton;
  private Button newServerButton;
  private Text serverUserTextField;
  private Text serverPasswordTextField;

  public abstract String getServerName();
  public abstract int getDefaultServerPort();
  public abstract String getDefaultServerUser();
  public abstract boolean checkForAppropriateRuntime(IRuntime runtime);

  private ServerConfigurator configurator;
  public ServerConfigurator getConfigurator() { return configurator; }

  public void initValues()
  {
    configurator = ServerConfigurator.Factory.create(getServerSettings());
    ContainerType type = ServerSettings.getContainerTypeFromClass(this.getClass().getSimpleName());
    if (type != null)
    {
      String prevServerHome = MdwPlugin.getStringPref(type + "-" + ProjectPersist.MDW_SERVER_HOME);
      if (prevServerHome.length() > 0)
        getServerSettings().setHome(prevServerHome);
      if (getServerSettings().getHome() != null)
        serverHomeTextField.setText(getServerSettings().getHome());

      if (targetRuntimeCombo != null)
      {
        String prevServerVer = MdwPlugin.getStringPref(type + "-" + ProjectPersist.MDW_SERVER_VERSION);
        if (prevServerVer.length() > 0)
        {
          getServerSettings().setContainerVersion(prevServerVer);
          for (IRuntime runtime : ServerCore.getRuntimes())
          {
            if (checkForAppropriateRuntime(runtime))
            {
              if (getServerSettings().getContainerVersion().equals(runtime.getRuntimeType().getVersion()))
                targetRuntimeCombo.setText(runtime.getName());
            }
          }
        }
      }
      else
      {
        // default version for service mix
        if (getServerSettings().isServiceMix())
          getServerSettings().setContainerVersion("4.4.1");
        if (getServerSettings().isFuse())
          getServerSettings().setContainerVersion("6.1.0");
      }

      String prevJdkHome = MdwPlugin.getStringPref(type + "-" + ProjectPersist.MDW_SERVER_JDK_HOME);
      if (prevJdkHome.length() > 0)
        getServerSettings().setJdkHome(prevJdkHome);
      if (getServerSettings().getJdkHome() != null)
        jdkHomeTextField.setText(getServerSettings().getJdkHome());

      String prevServerHost = MdwPlugin.getStringPref(type + "-" + ProjectPersist.MDW_SERVER_HOST);
      if (prevServerHost.length() > 0)
        getServerSettings().setHost(prevServerHost);
      else
        getServerSettings().setHost(ServerSettings.DEFAULT_HOST);
      serverHostTextField.setText(getServerSettings().getHost());

      String prevServerPort = MdwPlugin.getStringPref(type + "-" + ProjectPersist.MDW_SERVER_PORT);
      if (prevServerPort.length() > 0)
      {
        try
        {
          getServerSettings().setPort(Integer.parseInt(prevServerPort));
        }
        catch (NumberFormatException ex)
        {
          MdwPlugin.setStringPref(type + "-" + ProjectPersist.MDW_SERVER_PORT, String.valueOf(getDefaultServerPort()));
        }
      }
      else
      {
        getServerSettings().setPort(getDefaultServerPort());
      }
      serverPortTextField.setText(String.valueOf(getServerSettings().getPort()));

      String prevServerUser = MdwPlugin.getStringPref(type + "-" + ProjectPersist.MDW_SERVER_USER);
      if (prevServerUser.length() > 0)
        getServerSettings().setUser(prevServerUser);
      else
        getServerSettings().setUser(getDefaultServerUser());
      serverUserTextField.setText(getServerSettings().getUser());
    }

    if (!getProject().checkRequiredVersion(5, 0))
    {
      IProjectFacetVersion pfv = ProjectFacetsManager.getProjectFacet("mdw.workflow").getVersion("1.1");
      IFacetedProjectWorkingCopy wc = ((ModifyFacetedProjectWizard)getWizard()).getFacetedProjectWorkingCopy();
      wc.changeProjectFacetVersion(pfv);
    }
  }

  @Override
  /**
   * draw the widgets using a grid layout
   * @param parent - the parent composite
   */
  public void drawWidgets(Composite parent)
  {
    // create the composite to hold the widgets
    Composite composite = new Composite(parent, SWT.NULL);

    // create the layout for this wizard page
    GridLayout gl = new GridLayout();
    int ncol = 4;
    gl.numColumns = ncol;
    composite.setLayout(gl);

    Group group = new Group(composite, SWT.NONE);
    group.setText("Container");
    gl = new GridLayout();
    gl.numColumns = 3;
    group.setLayout(gl);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = 4;
    gd.grabExcessHorizontalSpace = true;
    group.setLayoutData(gd);

    createServerHomeControls(group, 3);
    if (getWizard() instanceof NewWorkflowProjectWizard)
      createRuntimeSelectionControls(group, 3);
    createJdkHomeControls(group, 3);

    group = new Group(composite, SWT.NONE);
    group.setText("Server");
    gl = new GridLayout();
    gl.numColumns = 4;
    group.setLayout(gl);
    gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = 4;
    gd.grabExcessHorizontalSpace = true;
    group.setLayoutData(gd);

    createServerHostControls(group, ncol);
    createServerPortControls(group, ncol);
    createServerLocControls(group, ncol);
    createAdditionalServerInfoControls(group, ncol);

    group = new Group(composite, SWT.NONE);
    group.setText(getServerName() + " User");
    gl = new GridLayout();
    gl.numColumns = 4;
    group.setLayout(gl);
    gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = 4;
    gd.grabExcessHorizontalSpace = true;
    group.setLayoutData(gd);

    createServerUserControls(group, ncol);

    createAdditionalControls(composite, ncol);

    setControl(composite);
  }

  protected ServerSettings getServerSettings()
  {
    if (getProject() == null)
      return null;
    else
      return getProject().getServerSettings();
  }

  protected void createServerHomeControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText(getServerName() + " Home:");
    serverHomeTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 300;
    serverHomeTextField.setLayoutData(gd);
    serverHomeTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getServerSettings().setHome(serverHomeTextField.getText().trim());
        handleFieldChanged();
      }
    });

    browseServerHomeButton = new Button(parent, SWT.PUSH);
    gd = new GridData(SWT.BEGINNING);
    browseServerHomeButton.setLayoutData(gd);
    browseServerHomeButton.setText("Browse...");
    browseServerHomeButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        DirectoryDialog dlg = new DirectoryDialog(getShell());
        dlg.setMessage("Select the directory where " + getServerName() + " is installed.");
        String serverHome = dlg.open();
        if (serverHome != null)
          serverHomeTextField.setText(serverHome);
      }
    });
  }

  protected void createRuntimeSelectionControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Target Runtime:");
    targetRuntimeCombo = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 287;
    targetRuntimeCombo.setLayoutData(gd);
    targetRuntimeCombo.removeAll();
    targetRuntimeCombo.add("");
    for (IRuntime runtime : ServerCore.getRuntimes())
    {
      if (runtime != null && runtime.getRuntimeType() != null && checkForAppropriateRuntime(runtime))
        targetRuntimeCombo.add(runtime.getName());
    }
    targetRuntimeCombo.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        IRuntime selectedRuntime = null;
        for (IRuntime runtime : ServerCore.getRuntimes())
        {
          if (targetRuntimeCombo.getText().equals(runtime.getName()))
            selectedRuntime = runtime;
        }
        if (selectedRuntime != null)
        {
          // only displayed for new workflow project wizard
          getServerSettings().setContainerVersion(selectedRuntime.getRuntimeType().getVersion());
          NewWorkflowProjectWizard wizard = (NewWorkflowProjectWizard)getWizard();
          wizard.getDataModel().setProperty(IFacetProjectCreationDataModelProperties.FACET_RUNTIME, RuntimeManager.getRuntime(selectedRuntime.getName()));
        }
        handleFieldChanged();
      }
    });

    newRuntimeButton = new Button(parent, SWT.PUSH);
    newRuntimeButton.setText("New...");
    newRuntimeButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        IDataModel dataModel = DataModelFactory.createDataModel(new EARFacetProjectCreationDataModelProvider());
        if (getWizard() instanceof NewWorkflowProjectWizard)
          dataModel = ((NewWorkflowProjectWizard)getWizard()).getDataModel();

        if (DataModelFacetCreationWizardPage.launchNewRuntimeWizard(getShell(), dataModel))
        {
          String[] existingRuntimeNames = targetRuntimeCombo.getItems();
          targetRuntimeCombo.removeAll();
          for (IRuntime runtime : ServerCore.getRuntimes())
          {
            if (checkForAppropriateRuntime(runtime))
              targetRuntimeCombo.add(runtime.getName());
          }
          String runtimeToSelect = "";
          for (String newRuntimeName : targetRuntimeCombo.getItems())
          {
            boolean found = false;
            for (String existingRuntimeName : existingRuntimeNames)
            {
              if (newRuntimeName.equals(existingRuntimeName))
              {
                found = true;
                break;
              }
            }
            if (!found)
            {
              runtimeToSelect = newRuntimeName;
              break;
            }
          }
          targetRuntimeCombo.setText(runtimeToSelect);
        }
      }
    });
  }

  protected void createJdkHomeControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Java Home:");
    jdkHomeTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 300;
    jdkHomeTextField.setLayoutData(gd);
    jdkHomeTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getServerSettings().setJdkHome(jdkHomeTextField.getText().trim());
        handleFieldChanged();
      }
    });

    browseJdkHomeButton = new Button(parent, SWT.PUSH);
    gd = new GridData(SWT.BEGINNING);
    browseJdkHomeButton.setLayoutData(gd);
    browseJdkHomeButton.setText("Browse...");
    browseJdkHomeButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        DirectoryDialog dlg = new DirectoryDialog(getShell());
        dlg.setMessage("Select the directory where the Server JDK is installed.");
        String jdkHome = dlg.open();
        if (jdkHome != null)
          jdkHomeTextField.setText(jdkHome);
      }
    });
  }

  protected void createServerHostControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Host:");
    serverHostTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 200;
    gd.horizontalSpan = ncol - 1;
    serverHostTextField.setLayoutData(gd);
    serverHostTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getServerSettings().setHost(serverHostTextField.getText().trim());
        handleFieldChanged();
      }
    });
  }

  protected void createServerPortControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Port:");

    serverPortTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 200;
    gd.horizontalSpan = ncol - 1;
    serverPortTextField.setLayoutData(gd);
    serverPortTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getServerSettings().setPort(Integer.parseInt(serverPortTextField.getText().trim()));
        handleFieldChanged();
      }
    });
  }

  private void createServerUserControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("User:");

    serverUserTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 150;
    serverUserTextField.setLayoutData(gd);
    serverUserTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getServerSettings().setUser(serverUserTextField.getText().trim());
        handleFieldChanged();
      }
    });

    new Label(parent, SWT.NONE).setText("      Password:");

    serverPasswordTextField = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 150;
    serverPasswordTextField.setLayoutData(gd);
    serverPasswordTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getServerSettings().setPassword(serverPasswordTextField.getText().trim());
        handleFieldChanged();
      }
    });
  }

  protected void createServerLocControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText(getServerLocationLabel() + ":");
    serverLocTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 300;
    gd.horizontalSpan = ncol - 3;
    serverLocTextField.setLayoutData(gd);
    serverLocTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getServerSettings().setServerLoc(serverLocTextField.getText().trim());
        parseServerAdditionalInfo(getProject().getServerSettings().getServerLoc());
        handleFieldChanged();
      }
    });

    browseServerLocButton = new Button(parent, SWT.PUSH);
    browseServerLocButton.setText("Browse...");
    browseServerLocButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        DirectoryDialog dlg = new DirectoryDialog(getShell());
        dlg.setMessage("Select the root directory of your " + getServerName() + " Server.");
        String serverLoc = dlg.open();
        if (serverLoc != null)
          serverLocTextField.setText(serverLoc);
      }
    });

    newServerButton = new Button(parent, SWT.PUSH);
    newServerButton.setText("New...");
    newServerButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        String serverHome = getProject().getServerSettings().getHome();
        if (!checkString(serverHome))
        {
          MessageDialog.openError(getShell(), "Missing Data", "Please enter a value for " + getServerName() + " Home.");
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

  protected void createAdditionalControls(Composite parent, int ncol)
  {
    // do nothing by default
  }

  protected void createAdditionalServerInfoControls(Composite parent, int ncol)
  {
    // do nothing by default
  }

  protected void parseServerAdditionalInfo(String serverLoc)
  {
    // do nothing by default
  }

  protected String getServerLocationLabel()
  {
    return "Server Location";
  }

  /**
   * @see WizardPage#getStatuses()
   */
  public IStatus[] getStatuses()
  {
    String msg = null;
    ServerSettings serverSettings = getProject().getServerSettings();

    String serverHomeMsg = serverHomeSpecializedCheck(serverSettings.getHome());
    String serverLocMsg = serverLocSpecializedCheck(serverSettings.getServerLoc());

    if (!checkDir(serverHomeTextField.getText().trim()))
      msg = "Please enter a valid directory for " + getServerName() + " Home";
    else if (containsWhitespace(serverHomeTextField.getText().trim()))
      msg = getServerName() + " Home must not contain whitespace characters";
    else if (serverHomeMsg != null)
      msg = serverHomeMsg;
    else if (getWizard() instanceof NewWorkflowProjectWizard && targetRuntimeCombo != null && targetRuntimeCombo.getText().length() == 0)
      msg = "Please select an appropriate Target Runtime.";
    else if (!checkDir(jdkHomeTextField.getText().trim()))
      msg = "Please enter a valid directory for JDK Home";
    else if (containsWhitespace(jdkHomeTextField.getText().trim()))
      msg = "JDK Home must not contain whitespace characters";
    else if (serverSettings.getJdkHome() != null && serverSettings.getJdkHome().length() != 0 && !checkFile(serverSettings.getJdkHome() + "/jre/lib/rt.jar"))
      msg = "JDK Home must contain jre/lib/rt.jar";
    else if (containsWhitespace(serverSettings.getHost()))
      msg = "Invalid value for " + getServerName() + " Server Host";
    else if (serverSettings.getPort() <= 0)
      msg = "Invalid value for " + getServerName() + " Server Port";
    else if (!checkDir(serverSettings.getServerLoc()) && serverLocTextField != null)
      msg = "Please enter a valid directory for " + getServerName() + " " + getServerLocationLabel();
    else if (containsWhitespace(serverSettings.getServerLoc()))
      msg = getServerName() + " " + getServerLocationLabel() + " must not contain whitespace characters";
    else if (serverLocMsg != null)
      msg = serverLocMsg;

    if (msg == null)
      return null;

    IStatus[] is = {new Status(IStatus.ERROR, getPluginId(), 0, msg, null)};
    return is;
  }

  protected String serverHomeSpecializedCheck(String serverHome)
  {
    return null;  // do nothing by default
  }

  protected String serverLocSpecializedCheck(String serverLoc)
  {
    return null;  // do nothing by default
  }

  /**
   * sets the completed field on the wizard class when all the information
   * on the page is entered
   */
  public boolean isPageComplete()
  {
    if (getStatuses() != null)
      return false;
    ServerSettings serverSettings = getProject().getServerSettings();
    return checkStringNoWhitespace(serverSettings.getHome())
      && checkStringNoWhitespace(serverSettings.getJdkHome())
      && checkStringNoWhitespace(serverSettings.getHost())
      && checkInt(serverSettings.getPort())
      && checkStringNoWhitespace(serverSettings.getServerLoc());
  }

  public void setWizardContext(IWizardContext context)
  {
  }

  public void transferStateToConfig()
  {
  }

  @Override
  public IWizardPage getNextPage()
  {
    PersistType persistType = getProject().getPersistType();
    for (IWizardPage page : getWizard().getPages())
    {
      if (page.getTitle().equals(DataSourcePage.PAGE_TITLE))
      {
        ((DataSourcePage)page).initValues(); // version specific
        return page;
      }
      else if (persistType == PersistType.Git && page.getTitle().equals(GitRepositoryPage.PAGE_TITLE))
      {
        ((GitRepositoryPage)page).initValues(); // version specific
        return page;
      }
      else if (persistType == PersistType.None)
      {
        return null;
      }
    }
    return super.getNextPage();
  }
}
