/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jst.common.project.facet.IJavaFacetInstallDataModelProperties;
import org.eclipse.jst.ejb.ui.project.facet.EjbProjectFirstPage;
import org.eclipse.jst.ejb.ui.project.facet.EjbProjectWizard;
import org.eclipse.jst.j2ee.ejb.project.operations.IEjbFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetProjectCreationDataModelProperties;
import org.eclipse.jst.j2ee.ui.project.facet.EarFacetInstallPage;
import org.eclipse.jst.j2ee.ui.project.facet.EarProjectFirstPage;
import org.eclipse.jst.j2ee.ui.project.facet.EarProjectWizard;
import org.eclipse.jst.j2ee.ui.project.facet.UtilityProjectWizard;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetProjectCreationDataModelProperties;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectTemplate;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DesignerPerspective;
import com.centurylink.mdw.plugin.designer.dialogs.MdwProgressMonitorDialog;
import com.centurylink.mdw.plugin.preferences.model.MdwSettings;
import com.centurylink.mdw.plugin.project.assembly.ExtensionModulesUpdater;
import com.centurylink.mdw.plugin.project.assembly.ProjectConfigurator;
import com.centurylink.mdw.plugin.project.assembly.ProjectInflator;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class NewWorkflowProjectWizard extends EarProjectWizard implements INewWizard
{
  public static final String WIZARD_ID = "mdw.designer.local.project";

  private MdwSettings mdwSettings = MdwPlugin.getSettings();

  private WorkflowProject project;
  public WorkflowProject getProject() { return project; }

  private WorkflowProjectPage workflowProjectPage;
  public WorkflowProjectPage getWorkflowProjectPage() { return workflowProjectPage; }

  private WebLogicSettingsPage webLogicSettingsPage;
  public WebLogicSettingsPage getWebLogicSettingsPage() { return webLogicSettingsPage; }

  private JBossSettingsPage jbossSettingsPage;
  public JBossSettingsPage getJBossSettingsPage() { return jbossSettingsPage; }

  private ServiceMixSettingsPage serviceMixSettingsPage;
  public ServiceMixSettingsPage getServiceMixSettingsPage() { return serviceMixSettingsPage; }

  private FuseSettingsPage fuseSettingsPage;
  public FuseSettingsPage getFuseSettingsPage() { return fuseSettingsPage; }

  private ExtensionModulesWizardPage extensionModulesPage;
  public ExtensionModulesWizardPage getExtensionModulesPage() { return extensionModulesPage; }

  private DataSourcePage dataSourcePage;
  public DataSourcePage getDataSourcePage() { return dataSourcePage; }

  public NewWorkflowProjectWizard()
  {
    setWindowTitle("New MDW Workflow Project");
  }

  private IWorkbenchWindow activeWindow;
  public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    super.init(workbench, selection);
    activeWindow = workbench.getActiveWorkbenchWindow();
  }

  protected IFacetedProjectTemplate getTemplate()
  {
    return ProjectFacetsManager.getTemplate("mdw.workflow.template");
  }

  public void addPages()
  {
    super.addPages();
    setDefaultPageImageDescriptor(MdwPlugin.getImageDescriptor("icons/mdw_wiz.png"));

    if (workflowProjectPage == null)
    {
      // use the preexisting workflow project
      for (IFacetedProject.Action action : getFacetedProjectWorkingCopy().getProjectFacetActions())
      {
        if (action.getType().equals(IFacetedProject.Action.Type.INSTALL)
            && action.getProjectFacetVersion().getProjectFacet().getId().equals("mdw.workflow"))
        {
          if (action.getConfig() instanceof WorkflowProject)
            project = (WorkflowProject) action.getConfig();
        }
      }
      if (project == null)
        project = (WorkflowProject) new WorkflowProject.Factory().create();

      project.setDefaultFilesToIgnoreDuringUpdate();
      project.setSkipFacetPostInstallUpdates(true);

      workflowProjectPage = new WorkflowProjectPage();
      workflowProjectPage.setConfig(project);
      addPage(workflowProjectPage);

      webLogicSettingsPage = new WebLogicSettingsPage();
      webLogicSettingsPage.setConfig(project);
      addPage(webLogicSettingsPage);

      jbossSettingsPage = new JBossSettingsPage();
      jbossSettingsPage.setConfig(project);
      addPage(jbossSettingsPage);

      serviceMixSettingsPage = new ServiceMixSettingsPage();
      serviceMixSettingsPage.setConfig(project);
      addPage(serviceMixSettingsPage);

      fuseSettingsPage = new FuseSettingsPage();
      fuseSettingsPage.setConfig(project);
      addPage(fuseSettingsPage);

      dataSourcePage = new DataSourcePage();
      dataSourcePage.setConfig(project);
      addPage(dataSourcePage);

      extensionModulesPage = new ExtensionModulesWizardPage();
      extensionModulesPage.setConfig(project);
      addPage(extensionModulesPage);
    }
  }

  @Override
  public IWizardPage[] getPages()
  {
    IWizardPage[] oldPages = super.getPages();
    IWizardPage[] newPages = null;

    // arrange the pages to our liking
    EarProjectFirstPage earProjectFirstPage = null;
    EarFacetInstallPage earFacetInstallPage = null;

    for (IWizardPage page : oldPages)
    {
      if (page instanceof EarProjectFirstPage)
        earProjectFirstPage = (EarProjectFirstPage)page;
      else if (page instanceof EarFacetInstallPage)
        earFacetInstallPage = (EarFacetInstallPage)page;
    }

    if (earFacetInstallPage == null)
    {
      newPages = new IWizardPage[7];
      newPages[0] = workflowProjectPage;
      newPages[1] = earProjectFirstPage;
      newPages[2] = webLogicSettingsPage;
      newPages[3] = jbossSettingsPage;
      newPages[4] = serviceMixSettingsPage;
      newPages[5] = fuseSettingsPage;
      newPages[6] = dataSourcePage;
    }
    else
    {
      newPages = new IWizardPage[9];
      newPages[0] = workflowProjectPage;
      newPages[1] = earProjectFirstPage;
      newPages[2] = earFacetInstallPage;
      newPages[3] = webLogicSettingsPage;
      newPages[4] = jbossSettingsPage;
      newPages[5] = serviceMixSettingsPage;
      newPages[6] = fuseSettingsPage;
      newPages[7] = dataSourcePage;
      newPages[8] = extensionModulesPage;
    }

    return newPages;
  }


  @Override
  public boolean performFinish()
  {
    project.setDefaultFilesToIgnoreDuringUpdate();  // can depend on container selected

    ProjectInflator projectInflator = new ProjectInflator(project, MdwPlugin.getSettings());

    if (project.isOsgi() || project.isWar())
    {
      project.setCloudProject(true);
      projectInflator.inflateCloudProject(getContainer());

      BusyIndicator.showWhile(getShell().getDisplay(), new Runnable()
      {
        public void run()
        {
          postInflate();
        }
      });
    }
    else
    {
      if (!super.performFinish())
        return false;

      if (project != null)
      {
        String earProjectName = (String) getDataModel().getProperty(IFacetProjectCreationDataModelProperties.FACET_PROJECT_NAME);
        project.setEarProjectName(earProjectName);

        // close this wizard dialog and proceed to next
        ((WizardDialog)getContainer()).close();

        // create the pojo/ejb project (web projects are part of extension module)
        boolean success = false;
        if (project.getSourceProjectType().equals(WorkflowProject.SourceProjectType.EJB))
          success = launchEjbProjectWizard(getShell(), project);
        else
          success = launchPojoProjectWizard(getShell(), project);

        if (success)
        {
          try
          {
            ProgressMonitorDialog pmDialog = new MdwProgressMonitorDialog(getShell());
            pmDialog.run(true, true, new IRunnableWithProgress()
            {
              public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
              {
                try
                {
                  monitor.beginTask("Creating project...", 250 + project.getExtensionModules().size() * 50);

                  WorkflowProjectManager wfProjectManager = WorkflowProjectManager.getInstance();
                  wfProjectManager.add(project, project.getEarProject());

                  ProjectInflator projectInflator = new ProjectInflator(project, mdwSettings);
                  projectInflator.inflate(getShell(), monitor);

                  wfProjectManager.save(project, project.getEarProject());
                  monitor.done();
                }
                catch (Exception ex)
                {
                  PluginMessages.log(ex);
                  throw new InvocationTargetException(ex);
                }
              }
            });

          }
          catch (InvocationTargetException ex)
          {
            PluginMessages.uiError(ex, "New Workflow Project", project);
            return false;
          }
          catch (InterruptedException ex)
          {
            PluginMessages.log(ex);
            MessageDialog.openError(MdwPlugin.getShell(), "New Workflow Project", "Operation cancelled");
            return false;
          }
          catch (OperationCanceledException ex)
          {
            PluginMessages.log(ex);
            MessageDialog.openError(MdwPlugin.getShell(), "New Workflow Project", "Operation cancelled");
            return false;
          }
          catch (Exception ex)
          {
            PluginMessages.uiError(ex, "New Workflow Project");
            return false;
          }

          // take care of any extension modules
          ExtensionModulesUpdater changer = new ExtensionModulesUpdater(getProject());
          changer.setAdds(project.getExtensionModules());
          changer.doChanges(getShell());

          postInflate();
        }
      }
    }

    return true;
  }

  private void postInflate()
  {
    if (!project.isRemote())
    {
      // remove the pesky temp (or workflow) folder from the deployment assembly and add the resources folder
      try
      {
        ProjectConfigurator projConf = new ProjectConfigurator(getProject(), MdwPlugin.getSettings());
        getProject().getSourceProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        if (project.isFilePersist())
          projConf.removeDeploymentAssemblyResourceMappings(project.getAssetFolder());
        else
          projConf.removeDeploymentAssemblyResourceMappings(project.getTempFolder());
        projConf.addDeploymentAssemblyResourceMappings(project.getSourceProject().getFolder("/src/main/resources"));
      }
      catch (CoreException ex)
      {
        PluginMessages.uiError(ex, "Inflate Project", getProject());
      }
    }

    DesignerPerspective.promptForShowPerspective(activeWindow, project);
  }

  private boolean launchEjbProjectWizard(Shell shell, WorkflowProject workflowProject)
  {
    EjbProjectWizard ejbProjectWizard = new EjbProjectWizard()
    {
      EjbProjectFirstPage firstPage;

      @Override
      protected IFacetedProjectTemplate getTemplate()
      {
        ServerSettings serverSettings = getProject().getServerSettings();
        String containerVersion = getProject().getServerSettings().getContainerVersion();
        if (serverSettings.isWebLogic() && containerVersion.equals("10.0")
            || serverSettings.isJBoss() && containerVersion.equals("4.2"))
        {
          return ProjectFacetsManager.getTemplate("mdw.ejb.template.java5");
        }
        else
        {
          return ProjectFacetsManager.getTemplate("mdw.ejb.template.java6");
        }
      }

      @Override
      public void addPages()
      {
        super.addPages();
        firstPage = new EjbProjectFirstPage(model, "MDW EJB Project")
        {
          @Override
          public IWizardPage getNextPage()
          {
            return null;
          }
        };
        addPage(firstPage);
      }

      @Override
      public IWizardPage[] getPages()
      {
        IWizardPage[] pages = super.getPages();
        pages[0] = firstPage;
        return pages;
      }
    };
    IDataModel dataModel = ejbProjectWizard.getDataModel();
    // data model props
    dataModel.setStringProperty(IFacetProjectCreationDataModelProperties.FACET_PROJECT_NAME, workflowProject.getSourceProjectName());
    dataModel.setBooleanProperty(IJ2EEFacetProjectCreationDataModelProperties.ADD_TO_EAR, true);
    dataModel.setStringProperty(IJ2EEFacetProjectCreationDataModelProperties.EAR_PROJECT_NAME, workflowProject.getEarProjectName());
    FacetDataModelMap facetDmMap = (FacetDataModelMap) ejbProjectWizard.getDataModel().getProperty(IFacetProjectCreationDataModelProperties.FACET_DM_MAP);
    // java facet data model props
    IDataModel javaFacetDataModel = (IDataModel) facetDmMap.get("jst.java");
    javaFacetDataModel.setStringProperty(IJavaFacetInstallDataModelProperties.SOURCE_FOLDER_NAME, "src");
    // ejb facet data model props
    IDataModel ejbFacetDataModel = (IDataModel) facetDmMap.get("jst.ejb");
    ejbFacetDataModel.setBooleanProperty(IEjbFacetInstallDataModelProperties.CREATE_CLIENT, false);
    ejbFacetDataModel.setStringProperty(IEjbFacetInstallDataModelProperties.CONFIG_FOLDER, "src");

    WizardDialog dialog = new WizardDialog(shell, ejbProjectWizard);
    if (dialog.open() != Dialog.OK)
      return false;

    // in case user changed the ejb project name
    String projectName = (String) ejbProjectWizard.getDataModel().getProperty(IFacetProjectCreationDataModelProperties.FACET_PROJECT_NAME);
    workflowProject.setSourceProjectName(projectName);
    return true;
  }

  private boolean launchPojoProjectWizard(Shell shell, WorkflowProject workflowProject)
  {
    UtilityProjectWizard javaProjectWizard = new UtilityProjectWizard();
    IDataModel dataModel = javaProjectWizard.getDataModel();
    // data model props
    dataModel.setStringProperty(IFacetProjectCreationDataModelProperties.FACET_PROJECT_NAME, workflowProject.getSourceProjectName());
    dataModel.setBooleanProperty(IJ2EEFacetProjectCreationDataModelProperties.ADD_TO_EAR, true);
    dataModel.setStringProperty(IJ2EEFacetProjectCreationDataModelProperties.EAR_PROJECT_NAME, workflowProject.getEarProjectName());
    FacetDataModelMap facetDmMap = (FacetDataModelMap) javaProjectWizard.getDataModel().getProperty(IFacetProjectCreationDataModelProperties.FACET_DM_MAP);
    // java facet data model props
    IDataModel javaFacetDataModel = (IDataModel) facetDmMap.get("jst.java");
    javaFacetDataModel.setStringProperty(IJavaFacetInstallDataModelProperties.SOURCE_FOLDER_NAME, "src");

    WizardDialog dialog = new WizardDialog(shell, javaProjectWizard);
    if (dialog.open() != Dialog.OK)
      return false;

    // in case user changed the java project name
    String projectName = (String) javaProjectWizard.getDataModel().getProperty(IFacetProjectCreationDataModelProperties.FACET_PROJECT_NAME);
    workflowProject.setSourceProjectName(projectName);
    return true;
  }

  @Override
  public boolean canFinish()
  {
    return workflowProjectPage.isPageComplete()
      && ( (project.getServerSettings().isJBoss() && jbossSettingsPage.isPageComplete())
            || (project.getServerSettings().isServiceMix() && serviceMixSettingsPage.isPageComplete())
            || (project.getServerSettings().isFuse() && fuseSettingsPage.isPageComplete())
            || (project.getServerSettings().isWebLogic() && webLogicSettingsPage.isPageComplete()))
      && dataSourcePage.isPageComplete();
  }

}
