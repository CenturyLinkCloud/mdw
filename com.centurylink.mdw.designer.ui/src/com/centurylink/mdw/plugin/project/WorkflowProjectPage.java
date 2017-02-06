/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jst.j2ee.ui.project.facet.EarProjectWizard;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetProjectCreationDataModelProperties;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.ui.IFacetWizardPage;
import org.eclipse.wst.common.project.facet.ui.IWizardContext;
import org.eclipse.wst.common.project.facet.ui.ModifyFacetedProjectWizard;

import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.ServerSettings.ContainerType;
import com.centurylink.mdw.plugin.project.model.VcsRepository;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.plugin.project.model.WorkflowProject.PersistType;

public class WorkflowProjectPage extends WizardPage implements IFacetWizardPage
{
  public static final String PAGE_TITLE = "MDW Workflow Project";
  // page widgets
  private Text sourceProjectNameTextField;
  private Combo mdwVersionComboBox;
  private Button pojoProjectRadioButton;
  private Button ejbProjectRadioButton;
  private Button serviceMixRadioButton;
  private Button fuseRadioButton;
  private Button tomcatRadioButton;
  private Button weblogicRadioButton;
  private Button persistDbRadioButton;
  private Button persistFilesRadioButton;
  private Button persistNoneRadioButton;

  public String getSourceProjectName()
  {
    if (sourceProjectNameTextField == null)
      return null;
    else
      return sourceProjectNameTextField.getText();
  }

  public WorkflowProjectPage()
  {
    setTitle(PAGE_TITLE);
    setDescription("Enter the name of your Workflow Project.\n");
  }

  public void createControl(Composite parent)
  {
    drawWidgets(parent);
  }

  protected ServerSettings getServerSettings()
  {
    if (getProject() == null)
      return null;
    else
      return getProject().getServerSettings();
  }

  /**
   * Draw the widgets using a grid layout.
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

    createSourceProjectControls(composite, ncol);
    if (!getProject().isRemote())
    {
      createMdwVersionControls(composite, ncol);
      createAuthorControls(composite, ncol);
      createSpacer(composite, ncol);
      createJavaContainerControls(composite, ncol);
      enableContainersBasedOnVersion();
      if (getProject().isCloudProject())
      {
        createPersistenceControls(composite, ncol);
        enablePersistenceBasedOnVersion();
      }
      else
      {
        createSourceProjectTypeControls(composite, ncol);
      }
      createSpacer(composite, ncol);
    }

    setControl(composite);
  }

  /**
   * @see WizardPage#getStatuses()
   */
  public IStatus[] getStatuses()
  {
    if (!getProject().isRemote() && containsWhitespace(getSourceProjectName()))
    {
      IStatus s = new Status(IStatus.ERROR, getPluginId(), 0, "Source project name cannot contain whitespace.", null);
      return new IStatus[] { s };
    }
    else if (WorkflowProjectManager.getInstance().projectNameExists(getSourceProjectName()))
    {
      IStatus s = new Status(IStatus.ERROR, getPluginId(), 0, "Source project name already exists.", null);
      return new IStatus[] { s };
    }
    else if (getProject().isEarProject())
    {
      // adding workflow facet to existing EAR project
      IJavaProject sourceProject = WorkflowProjectManager.getJavaProject(getSourceProjectName());
      if (!sourceProject.exists())
      {
        return new IStatus[] { new Status(IStatus.ERROR, getPluginId(), 0, "Source project name should refer to a Java project that exists in your workspace.", null) };
      }
      else
      {
        return new IStatus[] { new Status(IStatus.INFO, getPluginId(), 0, "Please ensure that Source project has either the 'EJB Module' facet or the 'Utility Module' facet\nand that it's included in the EAR project's deployment assembly.", null) };
      }
    }
    else
    {
      return null;
    }
  }

  /**
   * Sets the completed field on the wizard class when all the information
   * on the page is entered.
   */
  public boolean isPageComplete()
  {
    boolean complete = true;
    if (getProject().isRemote())
    {
      if (!checkString(getProject().getSourceProjectName()))
        complete = false;
    }
    else
    {
      if (!isValidContainerBasedOnVersion())
        complete = false;
      else if (!checkStringNoWhitespace(getProject().getSourceProjectName()))
        complete = false;
      else if (!checkStringNoWhitespace(getProject().getMdwVersion()))
        complete = false;
    }
    if (complete)
    {
      complete = !WorkflowProjectManager.getInstance().projectNameExists(getSourceProjectName());
    }
    if (complete && getProject().isEarProject())
    {
      // adding workflow facet to existing EAR project
      complete = WorkflowProjectManager.getJavaProject(getSourceProjectName()).exists();
    }
    return complete;
  }

  private void createSourceProjectControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Project Name:");

    sourceProjectNameTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 200;
    gd.horizontalSpan = ncol - 2;
    sourceProjectNameTextField.setLayoutData(gd);
    sourceProjectNameTextField.setTextLimit(50);
    final boolean updateEarBasedOnSource = getProject().getEarProjectName() == null;
    sourceProjectNameTextField.addModifyListener(new ModifyListener()
      {
        public void modifyText(ModifyEvent e)
        {
          String workflowProjectName = sourceProjectNameTextField.getText().trim();
          getProject().setSourceProjectName(workflowProjectName);
          if (updateEarBasedOnSource)
            getProject().setEarProjectName(workflowProjectName + "Ear");
          if (getWizard() instanceof EarProjectWizard)
          {
            EarProjectWizard earProjectWizard = (EarProjectWizard) getWizard();
            earProjectWizard.getDataModel().setProperty(IFacetProjectCreationDataModelProperties.FACET_PROJECT_NAME, getProject().getEarProjectName());
          }
          handleFieldChanged();
        }
      });
    // when activated by adding facet to existing cloud project
    if (getProject().isCloudProject() && getProject().getEarProjectName() != null)
    {
      getProject().setSourceProjectName(getProject().getEarProjectName());
      sourceProjectNameTextField.setText(getProject().getSourceProjectName());
      if (getProject().isCloudProject())
        sourceProjectNameTextField.setEditable(false);
    }

    Label existing = new Label(parent, SWT.BEGINNING);
    existing.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL));
    // adding facet to existing EAR project
    if (getProject().isEarProject())
    {
      existing.setText("  (Existing Java Project)");
      if (getProject().getEarProjectName() != null)
      {
        IProject proj = MdwPlugin.getWorkspaceRoot().getProject(getProject().getEarProjectName());
        if (proj != null)
        {
          IJavaProject relatedJavaProj = WorkflowProjectManager.getInstance().getRelatedJavaProject(proj);
          if (relatedJavaProj != null)
            sourceProjectNameTextField.setText(relatedJavaProj.getProject().getName());
        }
      }
    }
  }

  private void createSourceProjectTypeControls(Composite parent, int ncol)
  {
    Group radioGroup = new Group(parent, SWT.NONE);
    radioGroup.setText("Source Project Type");
    GridLayout gl = new GridLayout();
    radioGroup.setLayout(gl);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = ncol;
    gd.grabExcessHorizontalSpace = true;
    radioGroup.setLayoutData(gd);
    pojoProjectRadioButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
    pojoProjectRadioButton.setText("POJO Project");
    pojoProjectRadioButton.setSelection(true);
    pojoProjectRadioButton.addSelectionListener(new SelectionAdapter()
      {
        @Override
        public void widgetSelected(SelectionEvent e)
        {
          if (pojoProjectRadioButton.getSelection())
            getProject().setSourceProjectType(WorkflowProject.SourceProjectType.POJO);
          handleFieldChanged();
        }
      });
    ejbProjectRadioButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
    ejbProjectRadioButton.setText("EJB 3 Project");
    ejbProjectRadioButton.addSelectionListener(new SelectionAdapter()
      {
        @Override
        public void widgetSelected(SelectionEvent e)
        {
          if (ejbProjectRadioButton.getSelection())
            getProject().setSourceProjectType(WorkflowProject.SourceProjectType.EJB);
          handleFieldChanged();
        }
      });
    ejbProjectRadioButton.setEnabled(getServerSettings().isJavaEE() && !getProject().isCloudProject());
  }

  private void createMdwVersionControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("MDW Version:");
    mdwVersionComboBox = new Combo(parent, SWT.DROP_DOWN);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol - 1;
    mdwVersionComboBox.setLayoutData(gd);

    mdwVersionComboBox.removeAll();
    List<String> mdwVersions = MdwPlugin.getSettings().getMdwVersions();
    for (int i = 0; i < mdwVersions.size(); i++)
      mdwVersionComboBox.add(mdwVersions.get(i));

    // default to latest version
    String latestVersion = MdwPlugin.getSettings().getLatestMdwVersion();
    getProject().setMdwVersion(latestVersion);

    mdwVersionComboBox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        String mdwVersion = mdwVersionComboBox.getText();
        getProject().setMdwVersion(mdwVersion);
        enableContainersBasedOnVersion();
        enablePersistenceBasedOnVersion();
        handleFieldChanged();
      }
    });

    mdwVersionComboBox.setText(latestVersion);
  }

  private void createJavaContainerControls(Composite parent, int ncol)
  {
    Group radioGroup = new Group(parent, SWT.NONE);
    radioGroup.setText("Container");
    GridLayout gl = new GridLayout();
    radioGroup.setLayout(gl);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = ncol;
    gd.grabExcessHorizontalSpace = true;
    radioGroup.setLayoutData(gd);

    String prevServerType = MdwPlugin.getStringPref(ProjectPersist.MDW_SERVER_TYPE);
    if (prevServerType.length() > 0)
      getServerSettings().setContainerType(ContainerType.valueOf(prevServerType));

    serviceMixRadioButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
    serviceMixRadioButton.setText("Apache ServiceMix 4.4 (Requires MDW 5.2)");
    if (getServerSettings().isServiceMix())
      serviceMixRadioButton.setSelection(true);
    serviceMixRadioButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        getProject().clearServerSettings();
        if (serviceMixRadioButton.getSelection())
        {
          getServerSettings().setContainerType(ServerSettings.ContainerType.ServiceMix);
          if (pojoProjectRadioButton != null)
            pojoProjectRadioButton.setSelection(true);
          if (ejbProjectRadioButton != null)
          {
            ejbProjectRadioButton.setSelection(false);
            ejbProjectRadioButton.setEnabled(false);
          }
        }
        handleFieldChanged();
      }
    });

    fuseRadioButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
    fuseRadioButton.setText("JBoss Fuse 6.1 (Requires MDW 5.5)");
    if (getServerSettings().isFuse())
      fuseRadioButton.setSelection(true);
    fuseRadioButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        getProject().clearServerSettings();
        if (fuseRadioButton.getSelection())
        {
          getServerSettings().setContainerType(ServerSettings.ContainerType.Fuse);
          if (pojoProjectRadioButton != null)
            pojoProjectRadioButton.setSelection(true);
          if (ejbProjectRadioButton != null)
          {
            ejbProjectRadioButton.setSelection(false);
            ejbProjectRadioButton.setEnabled(false);
          }
        }
        handleFieldChanged();
      }
    });

    if (getProject().isCloudProject())
    {
      tomcatRadioButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
      tomcatRadioButton.setText("Apache Tomcat 7/8 (Requires MDW 5.5)");
      if (getServerSettings().isTomcat())
        tomcatRadioButton.setSelection(true);
      tomcatRadioButton.addSelectionListener(new SelectionAdapter()
      {
        @Override
        public void widgetSelected(SelectionEvent e)
        {
          getProject().clearServerSettings();
          if (tomcatRadioButton.getSelection())
          {
            getServerSettings().setContainerType(ServerSettings.ContainerType.Tomcat);
            if (pojoProjectRadioButton != null)
              pojoProjectRadioButton.setSelection(true);
            if (ejbProjectRadioButton != null)
            {
              ejbProjectRadioButton.setSelection(false);
              ejbProjectRadioButton.setEnabled(false);
            }
          }
          handleFieldChanged();
        }
      });
    }

    weblogicRadioButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
    weblogicRadioButton.setText("WebLogic Server 10.x (MDW 5.2 Only)");
    if (getServerSettings().getContainerType() == null || getServerSettings().isWebLogic())
      weblogicRadioButton.setSelection(true);
    weblogicRadioButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        getProject().clearServerSettings();
        if (weblogicRadioButton.getSelection())
        {
          getServerSettings().setContainerType(ServerSettings.ContainerType.WebLogic);
          if (ejbProjectRadioButton != null)
            ejbProjectRadioButton.setEnabled(true);
        }
        handleFieldChanged();
      }
    });
  }

  private void enableContainersBasedOnVersion()
  {
    // Fuse enablement is conditional
    if (fuseRadioButton != null)
    {
      if (StringHelper.isEmpty(getProject().getMdwVersion()) || !getProject().checkRequiredVersion(5, 5))
      {
        if (fuseRadioButton.getSelection())
        {
          fuseRadioButton.setSelection(false);
          serviceMixRadioButton.setSelection(true);
          getServerSettings().setContainerType(ContainerType.ServiceMix);
        }
        fuseRadioButton.setEnabled(false);
      }
      else
      {
        fuseRadioButton.setEnabled(true);
      }
    }
    // WebLogic enablement is conditional
    if (weblogicRadioButton != null)
    {
      if (StringHelper.isEmpty(getProject().getMdwVersion()) || getProject().checkRequiredVersion(5, 5)
          || getWizard() instanceof LocalCloudProjectWizard)
      {
        if (weblogicRadioButton.getSelection())
        {
          weblogicRadioButton.setSelection(false);
          serviceMixRadioButton.setSelection(true);
          getServerSettings().setContainerType(ContainerType.ServiceMix);
        }
        weblogicRadioButton.setEnabled(false);
      }
      else
      {
        weblogicRadioButton.setEnabled(true);
      }
    }
  }

  private boolean isValidContainerBasedOnVersion()
  {
    ContainerType container = getProject().getServerSettings().getContainerType();
    if (container == null)
      return false;

    String mdwVersion = getProject().getMdwVersion();
    if (mdwVersion == null || mdwVersion.isEmpty())
      return false;

    if (getProject().checkRequiredVersion(5, 5))
    {
      if (container == ContainerType.WebLogic && getWizard() instanceof LocalCloudProjectWizard)
        return false;
    }
    else
    {
      if (container == ContainerType.Fuse || container == ContainerType.Tomcat)
        return false;
    }

    return true;
  }

  private void enablePersistenceBasedOnVersion()
  {
    // file persist is conditional
    if (persistFilesRadioButton != null)
    {
      if (StringHelper.isEmpty(getProject().getMdwVersion()) || !getProject().checkRequiredVersion(5, 5))
      {
        if (persistFilesRadioButton.getSelection())
        {
          persistFilesRadioButton.setSelection(false);
          persistDbRadioButton.setSelection(true);
          getProject().setPersistType(PersistType.Database);
        }
        persistFilesRadioButton.setEnabled(false);
      }
      else
      {
        persistFilesRadioButton.setEnabled(true);
      }
    }
  }

  private void createPersistenceControls(Composite parent, int ncol)
  {
    Group radioGroup = new Group(parent, SWT.NONE);
    radioGroup.setText("Asset Persistence");
    GridLayout gl = new GridLayout();
    radioGroup.setLayout(gl);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = ncol;
    gd.grabExcessHorizontalSpace = true;
    radioGroup.setLayoutData(gd);

    String prevPersist = MdwPlugin.getStringPref(ProjectPersist.MDW_PERSIST_TYPE);
    if (prevPersist.length() > 0)
    {
      getProject().setPersistType(PersistType.valueOf(prevPersist));
    }
    else if (getProject().checkRequiredVersion(5, 5))
    {
      // vcs is now the default
      getProject().setPersistType(PersistType.Git);
      getProject().getMdwVcsRepository().setProvider(VcsRepository.PROVIDER_GIT);
    }

    persistDbRadioButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
    persistDbRadioButton.setText("Database (Oracle or MySQL)");
    if (getProject().getPersistType() == PersistType.Database)
      persistDbRadioButton.setSelection(true);
    persistDbRadioButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        if (persistDbRadioButton.getSelection())
          getProject().setPersistType(PersistType.Database);
        handleFieldChanged();
      }
    });

    persistFilesRadioButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
    persistFilesRadioButton.setText("VCS Repository (Such as Git)");
    if (getProject().getPersistType() == PersistType.Git || getProject().checkRequiredVersion(5, 5))
      persistFilesRadioButton.setSelection(true);
    persistFilesRadioButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        if (persistFilesRadioButton.getSelection())
        {
          getProject().setPersistType(PersistType.Git);
          getProject().getMdwVcsRepository().setProvider(VcsRepository.PROVIDER_GIT);
        }
        handleFieldChanged();
      }
    });

    persistNoneRadioButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
    persistNoneRadioButton.setText("None (Local Files Only)");
    if (getProject().getPersistType() == PersistType.None)
      persistNoneRadioButton.setSelection(true);
    persistNoneRadioButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        if (persistNoneRadioButton.getSelection())
          getProject().setPersistType(PersistType.None);
        handleFieldChanged();
      }
    });
  }

  public void setWizardContext(IWizardContext wizardContext)
  {
    IProjectFacetVersion workflowProjectFacetVersion = null;
    boolean hasJava16Facet = false;

    for (Object selectedFacetVersion : wizardContext.getSelectedProjectFacets())
    {
      IProjectFacetVersion projectFacetVersion = (IProjectFacetVersion) selectedFacetVersion;
      if (projectFacetVersion.getProjectFacet().getId().equals("mdw.workflow"))
        workflowProjectFacetVersion = projectFacetVersion;
      else if (projectFacetVersion.getProjectFacet().getId().equals("java") && Float.parseFloat(projectFacetVersion.getVersionString()) >= 1.6)
        hasJava16Facet = true;
    }

    if (workflowProjectFacetVersion != null)
    {
      setProject((WorkflowProject)wizardContext.getAction(IFacetedProject.Action.Type.INSTALL, workflowProjectFacetVersion).getConfig());
      if (hasJava16Facet)
      {
        getProject().setCloudProject(true);
      }
    }
  }

  public void transferStateToConfig()
  {
  }

  @Override
  public IWizardPage getNextPage()
  {
    if (getProject().isRemote())
    {
      RemoteWorkflowProjectWizard wizard = (RemoteWorkflowProjectWizard) getWizard();
      return wizard.getRemoteHostInfoPage();
    }
    else if (getWizard() instanceof LocalCloudProjectWizard)
    {
      // cloud or ear from new project wizard
      ServerSettingsPage serverSettingsPage = null;
      if (getProject().isCloudProject())
      {
        LocalCloudProjectWizard wizard = (LocalCloudProjectWizard) getWizard();
        if (getServerSettings().isJBoss())
          serverSettingsPage = wizard.getJBossSettingsPage();
        else if (getServerSettings().isServiceMix())
          serverSettingsPage = wizard.getServiceMixSettingsPage();
        else if (getServerSettings().isFuse())
          serverSettingsPage = wizard.getFuseSettingsPage();
        else if (getServerSettings().isTomcat())
          serverSettingsPage = wizard.getTomcatSettingsPage();
        else
          serverSettingsPage = wizard.getWebLogicSettingsPage();

        if (wizard.getExtensionModulesPage() != null)
          wizard.getExtensionModulesPage().initValues();
      }
      serverSettingsPage.initValues();
      return serverSettingsPage;
    }
    else
    {
      // is the case when adding workflow facet to existing EAR or Java project
      ModifyFacetedProjectWizard wizard = (ModifyFacetedProjectWizard) getWizard();
      ServerSettingsPage serverSettingsPage = null;

      for (IWizardPage page : wizard.getPages())
      {
        if (getServerSettings().isJBoss())
        {
          if (page.getTitle().equals(JBossSettingsPage.PAGE_TITLE))
            serverSettingsPage = (ServerSettingsPage)page;
        }
        else if (getServerSettings().isServiceMix())
        {
          if (page.getTitle().equals(ServiceMixSettingsPage.PAGE_TITLE))
            serverSettingsPage = (ServerSettingsPage)page;
        }
        else if (getServerSettings().isFuse())
        {
          if (page.getTitle().equals(FuseSettingsPage.PAGE_TITLE))
            serverSettingsPage = (ServerSettingsPage)page;
        }
        else if (getServerSettings().isTomcat())
        {
          if (page.getTitle().equals(TomcatSettingsPage.PAGE_TITLE))
            serverSettingsPage = (ServerSettingsPage)page;
        }
        else
        {
          if (page.getTitle().equals(WebLogicSettingsPage.PAGE_TITLE))
            serverSettingsPage = (ServerSettingsPage)page;
        }

        if (page.getTitle().equals(ExtensionModulesWizardPage.PAGE_TITLE))
          ((ExtensionModulesWizardPage)page).initValues();
      }
      if (serverSettingsPage != null)
      {
        serverSettingsPage.initValues();
        return serverSettingsPage;
      }
      else
      {
        return super.getNextPage();
      }
    }
  }
}
