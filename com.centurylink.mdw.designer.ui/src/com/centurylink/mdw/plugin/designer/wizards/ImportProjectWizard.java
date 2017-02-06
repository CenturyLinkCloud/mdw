/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DesignerPerspective;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.assembly.ProjectImporter;
import com.centurylink.mdw.plugin.project.assembly.ProjectInflator;
import com.centurylink.mdw.plugin.project.model.VcsRepository;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.plugin.project.model.WorkflowProject.PersistType;
import com.centurylink.mdw.workflow.WorkflowApplication;

public class ImportProjectWizard extends Wizard implements IImportWizard
{
  private ImportProjectPage importProjectPage;
  private ImportProjectSelectPage importProjectSelectPage;
  public ImportProjectSelectPage getProjectSelectPage() { return importProjectSelectPage; }

  private List<WorkflowProject> projectList;
  public List<WorkflowProject> getProjectList() { return projectList; }
  public void setProjectList(List<WorkflowProject> projects) { this.projectList = projects; }

  private List<WorkflowProject> projectsToImport;
  public List<WorkflowProject> getProjectsToImport() { return projectsToImport; }
  public void setProjectsToImport(List<WorkflowProject> projects) { this.projectsToImport = projects; }

  private WorkflowApplication discoveredWorkflowApp;
  public WorkflowApplication getDiscoveredWorkflowApp() { return discoveredWorkflowApp; }
  public void setDiscoveredWorkflowApp(WorkflowApplication workflowApp) { discoveredWorkflowApp = workflowApp; }

  private String errorMessage;
  public String getErrorMessage() { return errorMessage; }
  public void setErrorMessage(String msg) { this.errorMessage = msg; }

  private boolean cancel;

  private IWorkbenchWindow activeWindow;
  public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    setDefaultPageImageDescriptor(MdwPlugin.getImageDescriptor("icons/mdw_wiz.png"));
    activeWindow = workbench.getActiveWorkbenchWindow();
    projectList = new ArrayList<WorkflowProject>();
  }

  @Override
  public void addPages()
  {
    importProjectPage = new ImportProjectPage();
    addPage(importProjectPage);
    importProjectSelectPage = new ImportProjectSelectPage();
    addPage(importProjectSelectPage);
  }

  public void initializeProjectSelectPage()
  {
    importProjectSelectPage.initialize();
  }

  @Override
  public boolean performFinish()
  {
    if (errorMessage != null)
    {
      MessageDialog.openError(getShell(), "Import Project", errorMessage);
      return false;
    }

    BusyIndicator.showWhile(getShell().getDisplay(), new Runnable()
    {
      public void run()
      {
        List<IProject> existingProjects = new ArrayList<IProject>();
        for (WorkflowProject toImport : projectsToImport)
        {
          IProject existing = MdwPlugin.getWorkspaceRoot().getProject(toImport.getName());
          if (existing != null && existing.exists())
            existingProjects.add(existing);
        }

        if (!existingProjects.isEmpty())
        {
          String text = "Please confirm that the following workspace projects should be overwritten:";
          ListSelectionDialog lsd = new ListSelectionDialog(getShell(), existingProjects, new ExistingProjectContentProvider(), new ProjectLabelProvider(), text);
          lsd.setTitle("Existing Projects");
          lsd.setInitialSelections(existingProjects.toArray(new IProject[0]));
          lsd.open();
          Object[] results = (Object[]) lsd.getResult();
          if (results == null)
          {
            cancel = true;
            return;
          }

          for (IProject existing : existingProjects)
          {
            boolean include = false;
            for (Object included : results)
            {
              if (existing.getName().equals(((IProject)included).getName()))
                include = true;
            }
            if (include)
            {
              WorkflowProjectManager.getInstance().deleteProject(existing);
            }
            else
            {
              WorkflowProject toRemove = null;
              for (WorkflowProject wfp : projectList)
              {
                if (wfp.getName().equals(existing.getName()))
                {
                  toRemove = wfp;
                  break;
                }
              }
              if (toRemove != null)
                projectsToImport.remove(toRemove);
            }
          }
        }
      }
    });

    if (cancel)
      return false;

    if (projectsToImport.isEmpty())
    {
      MessageDialog.openInformation(getShell(), "Import Projects", "No projects to import.");
      return true;
    }

    try
    {
      getContainer().run(false, false, new IRunnableWithProgress()
      {
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
          monitor.beginTask("Importing MDW project(s)", 100);
          monitor.worked(20);
          try
          {
            for (WorkflowProject workflowProject : projectsToImport)
            {
              if (workflowProject.isFilePersist())
              {
                File projectDir = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile() + "/" + workflowProject.getName());
                projectDir.mkdir();
                String repositoryUrl = projectsToImport.get(0).getMdwVcsRepository().getRepositoryUrl();
                if (repositoryUrl != null && repositoryUrl.length() > 0)
                {
                  Platform.getBundle("org.eclipse.egit.ui").start(); // avoid Eclipse default Authenticator
                  workflowProject.setPersistType(PersistType.Git);
                  workflowProject.getMdwVcsRepository().setProvider(VcsRepository.PROVIDER_GIT);
                  monitor.subTask("Cloning Git repository for " + workflowProject.getLabel());
                  monitor.worked(1);
                  VcsRepository gitRepo = workflowProject.getMdwVcsRepository();
                  VersionControlGit vcsGit = new VersionControlGit();
                  String user = null;
                  String password = null;
                  if (MdwPlugin.getSettings().isUseDiscoveredVcsCredentials())
                  {
                    user = gitRepo.getUser();
                    password = gitRepo.getPassword();
                  }
                  vcsGit.connect(gitRepo.getRepositoryUrl(), user, password, projectDir);
                  vcsGit.cloneRepo();
                }
                else
                {
                  File assetDir = new File(projectDir + "/" + workflowProject.getMdwVcsRepository().getLocalPath());
                  assetDir.mkdirs();
                }
                monitor.worked(40 / projectsToImport.size());
              }
              ProjectInflator inflator = new ProjectInflator(workflowProject, MdwPlugin.getSettings());
              inflator.inflateRemoteProject(getContainer());
            }
          }
          catch (Exception ex)
          {
            throw new InvocationTargetException(ex);
          }

          ProjectImporter projectImporter = new ProjectImporter(projectsToImport);
          projectImporter.doImport();
          monitor.worked(20);
          monitor.done();
        }
     });
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Import Project");
      return false;
    }

    DesignerPerspective.openPerspective(activeWindow);
    return true;
  }

  @Override
  public boolean needsProgressMonitor()
  {
    return true;
  }

  class ExistingProjectContentProvider implements IStructuredContentProvider
  {
    @SuppressWarnings("unchecked")
    public Object[] getElements(Object inputElement)
    {
      List<IProject> wfProjects = (List<IProject>) inputElement;
      return wfProjects.toArray(new IProject[0]);
    }

    public void dispose()
    {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
    }
  }

  class ProjectLabelProvider implements ILabelProvider
  {
    ImageDescriptor descriptor = MdwPlugin.getImageDescriptor("icons/remote_project.gif");
    Image image;

    public ProjectLabelProvider()
    {
      image = descriptor.createImage();
    }

    public Image getImage(Object element)
    {
      return image;
    }

    public String getText(Object element)
    {
      if (element instanceof WorkflowProject)
        return ((WorkflowProject)element).getName();
      else if (element instanceof IProject)
        return ((IProject)element).getName();
      else
        return element.toString();
    }

    public void addListener(ILabelProviderListener listener)
    {
    }

    public void dispose()
    {
      if (image != null)
        image.dispose();
    }

    public boolean isLabelProperty(Object element, String property)
    {
      return false;
    }

    public void removeListener(ILabelProviderListener listener)
    {
    }
  }
}
