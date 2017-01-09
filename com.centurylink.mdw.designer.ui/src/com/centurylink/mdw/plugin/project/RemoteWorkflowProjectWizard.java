/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;

import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DesignerPerspective;
import com.centurylink.mdw.plugin.project.assembly.ProjectInflator;
import com.centurylink.mdw.plugin.project.model.VcsRepository;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.plugin.project.model.WorkflowProject.PersistType;

public class RemoteWorkflowProjectWizard extends Wizard implements INewWizard
{
  public static final String WIZARD_ID = "mdw.designer.remote.project";

  private WorkflowProjectPage workflowProjectPage;
  public WorkflowProjectPage getWorkflowProjectPage() { return workflowProjectPage; }

  private RemoteHostInfoPage remoteHostInfoPage;
  public RemoteHostInfoPage getRemoteHostInfoPage() { return remoteHostInfoPage; }

  private GitRepositoryPage gitRepositoryPage;
  public GitRepositoryPage getGitRepositoryPage() { return gitRepositoryPage; }

  private DataSourcePage dataSourcePage;
  public DataSourcePage getDataSourcePage() { return dataSourcePage; }

  private WorkflowProject workflowProject;
  public WorkflowProject getProject() { return workflowProject; }
  public void setProject(WorkflowProject project) { this.workflowProject = project; }

  private IWorkbenchWindow activeWindow;

  public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    activeWindow = workbench.getActiveWorkbenchWindow();
  }

  @Override
  public void addPages()
  {
    setDefaultPageImageDescriptor(MdwPlugin.getImageDescriptor("icons/mdw_wiz.png"));

    workflowProject = (WorkflowProject) new WorkflowProject.Factory().create();
    workflowProject.setRemote(true);

    workflowProjectPage = new WorkflowProjectPage();
    workflowProjectPage.setConfig(workflowProject);
    addPage(workflowProjectPage);

    remoteHostInfoPage = new RemoteHostInfoPage();
    remoteHostInfoPage.setConfig(workflowProject);
    addPage(remoteHostInfoPage);

    gitRepositoryPage = new GitRepositoryPage();
    gitRepositoryPage.setConfig(workflowProject);
    addPage(gitRepositoryPage);

    dataSourcePage = new DataSourcePage();
    dataSourcePage.setConfig(workflowProject);
    addPage(dataSourcePage);
  }

  @Override
  public boolean performFinish()
  {
    try
    {
      getContainer().run(false, false, new IRunnableWithProgress()
      {
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
           monitor.beginTask("Creating workflow project", 100);
           if (workflowProject.isFilePersist())
           {
             try
             {
               File projectDir = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile() + "/" + workflowProject.getName());
               projectDir.mkdir();
               String repositoryUrl = workflowProject.getMdwVcsRepository().getRepositoryUrl();
               if (repositoryUrl != null && repositoryUrl.length() > 0)
               {
                 workflowProject.setPersistType(PersistType.Git);
                 workflowProject.getMdwVcsRepository().setProvider(VcsRepository.PROVIDER_GIT);
                 monitor.subTask("Cloning Git repository");
                 monitor.worked(10);
                 Platform.getBundle("org.eclipse.egit.ui").start(); // avoid Eclipse default Authenticator
                 monitor.worked(10);
                 VcsRepository gitRepo = workflowProject.getMdwVcsRepository();
                 VersionControlGit vcsGit = new VersionControlGit();
                 vcsGit.connect(gitRepo.getRepositoryUrl(), gitRepo.getUser(), gitRepo.getPassword(), projectDir);
                 vcsGit.cloneRepo();
               }
               else
               {
                 File assetDir = new File(projectDir + "/" + workflowProject.getMdwVcsRepository().getLocalPath());
                 assetDir.mkdirs();
               }
             }
             catch (Exception ex)
             {
               throw new InvocationTargetException(ex);
             }
           }
           monitor.worked(50);
           ProjectInflator projectInflator = new ProjectInflator(workflowProject, MdwPlugin.getSettings());
           projectInflator.inflateRemoteProject(getContainer());
           WorkflowProjectManager.addProject(workflowProject);
           monitor.worked(25);

           monitor.done();
        }
     });
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Remote Project", workflowProject);
      return false;
    }

    DesignerPerspective.promptForShowPerspective(activeWindow, workflowProject);
    return true;
  }

  @Override
  public boolean needsProgressMonitor()
  {
    return true;
  }

}
