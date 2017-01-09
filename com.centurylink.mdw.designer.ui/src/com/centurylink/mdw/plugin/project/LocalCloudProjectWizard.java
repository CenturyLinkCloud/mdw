/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DesignerPerspective;
import com.centurylink.mdw.plugin.project.assembly.ExtensionModulesUpdater;
import com.centurylink.mdw.plugin.project.assembly.ProjectConfigurator;
import com.centurylink.mdw.plugin.project.assembly.ProjectInflator;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.plugin.project.model.WorkflowProject.PersistType;

public class LocalCloudProjectWizard extends Wizard implements INewWizard
{
  public static final String WIZARD_ID = "mdw.designer.cloud.project";

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

  private TomcatSettingsPage tomcatSettingsPage;
  public TomcatSettingsPage getTomcatSettingsPage() { return tomcatSettingsPage; }

  private ExtensionModulesWizardPage extensionModulesPage;
  public ExtensionModulesWizardPage getExtensionModulesPage() { return extensionModulesPage; }

  private DataSourcePage dataSourcePage;
  public DataSourcePage getDataSourcePage() { return dataSourcePage; }

  private GitRepositoryPage gitRepositoryPage;
  public GitRepositoryPage getGitRepositoryPage() { return gitRepositoryPage; }

  private WorkflowProject project;
  public WorkflowProject getProject() { return project; }
  public void setProject(WorkflowProject project) { this.project = project; }

  private IWorkbenchWindow activeWindow;

  public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    activeWindow = workbench.getActiveWorkbenchWindow();
  }

  @Override
  public void addPages()
  {
    setDefaultPageImageDescriptor(MdwPlugin.getImageDescriptor("icons/mdw_wiz.png"));

    project = (WorkflowProject) new WorkflowProject.Factory().create();
    project.setCloudProject(true);

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

    tomcatSettingsPage = new TomcatSettingsPage();
    tomcatSettingsPage.setConfig(project);
    addPage(tomcatSettingsPage);

    dataSourcePage = new DataSourcePage();
    dataSourcePage.setConfig(project);
    addPage(dataSourcePage);

    gitRepositoryPage = new GitRepositoryPage();
    gitRepositoryPage.setConfig(project);
    // addPage(gitRepositoryPage);

    extensionModulesPage = new ExtensionModulesWizardPage();
    extensionModulesPage.setConfig(project);
    addPage(extensionModulesPage);
  }


  @Override
  public boolean canFinish()
  {
    return workflowProjectPage.isPageComplete()
      && ( (project.getServerSettings().isJBoss() && jbossSettingsPage.isPageComplete())
            || (project.getServerSettings().isServiceMix() && serviceMixSettingsPage.isPageComplete())
            || (project.getServerSettings().isFuse() && fuseSettingsPage.isPageComplete())
            || (project.getServerSettings().isWebLogic() && webLogicSettingsPage.isPageComplete())
            || (project.getServerSettings().isTomcat() && tomcatSettingsPage.isPageComplete()) )
      && ( project.getPersistType() == PersistType.None
            || (project.getPersistType() == PersistType.Git ? gitRepositoryPage.isPageComplete() && dataSourcePage.isPageComplete() : dataSourcePage.isPageComplete()) );
  }

  @Override
  public boolean performFinish()
  {
    project.setDefaultFilesToIgnoreDuringUpdate();  // can depend on container selected

    ProjectInflator projectInflator = new ProjectInflator(project, MdwPlugin.getSettings());
    projectInflator.inflateCloudProject(getContainer());
    WorkflowProjectManager.addProject(project);

    // take care of any extension modules
    ExtensionModulesUpdater changer = new ExtensionModulesUpdater(getProject());
    changer.setAdds(project.getExtensionModules());
    changer.doChanges(getShell());

    if (!project.isRemote() && !project.isWar())
    {
      BusyIndicator.showWhile(getShell().getDisplay(), new Runnable()
      {
        public void run()
        {
          // remove the pesky temp (or workflow) folder from the deployment assembly, and add the resources folder
          try
          {
            ProjectConfigurator projConf = new ProjectConfigurator(getProject(), MdwPlugin.getSettings());
            getProject().getSourceProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
            if (getProject().isFilePersist())
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
      });
    }

    DesignerPerspective.promptForShowPerspective(activeWindow, project);
    return true;
  }
}
