/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.xmlbeans.XmlException;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.internal.core.refactoring.resource.RenameResourceProcessor;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.centurylink.mdw.auth.Authenticator;
import com.centurylink.mdw.auth.MdwSecurityException;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.User;
import com.centurylink.mdw.plugin.designer.DiscoveryException;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;
import com.centurylink.mdw.plugin.project.extensions.ExtensionModule;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.workflow.ConfigManagerProjectsDocument;
import com.centurylink.mdw.workflow.WorkflowApplication;

@SuppressWarnings("restriction")
public class WorkflowProjectManager implements IResourceChangeListener
{
  public static WorkflowProjectManager instance;
  public static WorkflowProjectManager getInstance()
  {
    if (instance == null)
      instance = new WorkflowProjectManager();
    return instance;
  }

  private WorkflowProjectManager() { }  // singleton

  public List<WorkflowProject> workflowProjects;
  public List<WorkflowProject> getWorkflowProjects()
  {
    if (workflowProjects == null)
      workflowProjects = loadWorkflowProjects();
    return workflowProjects;
  }

  /**
   * Clears the workflow projects.  Subsequent call to getWorkflowProjects() will reload.
   */
  public void refresh()
  {
    PluginMessages.log("WorkflowProjectManager refreshed", IStatus.INFO);
    if (workflowProjects != null)
    {
      for (WorkflowProject workflowProject : workflowProjects)
        workflowProject.clear();
    }
    workflowProjects = null;
  }

  public boolean isLoaded()
  {
    return workflowProjects != null;
  }

  private List<WorkflowProject> loadWorkflowProjects()
  {
    List<WorkflowProject> workflowProjects = new ArrayList<WorkflowProject>();
    List<IProject> javaSourceProjectsForEars = new ArrayList<IProject>();
    try
    {
      // local workflow projects (ear and cloud projects)
      for (IFacetedProject facetedProject : findWorkflowFacetedProjects())
      {
        if (facetedProject.getProject().isOpen())
        {
          boolean hasEarFacet = hasEarFacet(facetedProject.getProject());
          WorkflowProject wfProject = loadWorkflowProject(facetedProject.getProject());
          if (hasEarFacet)
          {
            wfProject.setEarProjectName(facetedProject.getProject().getName());
            javaSourceProjectsForEars.add(wfProject.getSourceProject());
          }
          else
          {
            wfProject.setCloudProject(true);
          }

          workflowProjects.add(wfProject);
        }
      }
    }
    catch (CoreException ex)
    {
      PluginMessages.log(ex);
    }

    // remote workflow projects
    workflowProjects.addAll(findRemoteWorkflowProjects(javaSourceProjectsForEars));

    Collections.sort(workflowProjects);

    for (WorkflowProject workflowProject : workflowProjects)
      workflowProject.initNoticeChecks();

    PluginMessages.log("WorkflowProjectManager loaded", IStatus.INFO);
    return workflowProjects;
  }

  /**
   * Finds all the MDW workflow faceted ear and java projects in the workspace.
   * @return list of faceted projects.
   */
  private List<IFacetedProject> findWorkflowFacetedProjects()
  {
    List<IFacetedProject> workflowFacetedProjects = new ArrayList<IFacetedProject>();

    try
    {
      for (IFacetedProject facetedProject : ProjectFacetsManager.getFacetedProjects())
      {
        boolean hasMdwFacet = false;
        for (IProjectFacetVersion projectFacetVersion : facetedProject.getProjectFacets())
        {
          IProjectFacet projectFacet = projectFacetVersion.getProjectFacet();
          if (projectFacet.getId().equals("mdw.workflow"))
            hasMdwFacet = true;
        }
        if (!workflowFacetedProjects.contains(facetedProject) && hasMdwFacet)
          workflowFacetedProjects.add(facetedProject);
      }
    }
    catch (CoreException ex)
    {
      PluginMessages.log(ex);
    }

    return workflowFacetedProjects;
  }

  /**
   * Instantiate a workflow project based on an Eclipse workspace project
   * @param project the ear or java faceted project to build the wf project from
   */
  private WorkflowProject loadWorkflowProject(IProject wfFacetedProject) throws CoreException
  {
    if (wfFacetedProject == null)
      return null;

    return create(wfFacetedProject);
  }

  /**
   * Gets the workflow project corresponding to the specified ear, source, or web project.
   */
  public WorkflowProject getWorkflowProject(IProject project)
  {
    for (WorkflowProject workflowProject : getWorkflowProjects())
    {
      if (project.getName().equals(workflowProject.getWebProjectName())) // not only ear projects can have webProject attr
      {
        return workflowProject;
      }
      else if (workflowProject.isRemote() || workflowProject.isCloudProject())
      {
        if (workflowProject.getName().equals(project.getName()))
          return workflowProject;
      }
      else if (workflowProject.getEarProjectName().equals(project.getName())
          || workflowProject.getSourceProjectName().equals(project.getName())
          || (workflowProject.getWebProjectName() != null && workflowProject.getWebProjectName().equals(project.getName())))
      {
        return workflowProject;
      }
    }
    return null;
  }

  /**
   * Find the workflow project (if any) that corresponds to the specified structured selection.
   */
  public WorkflowProject findWorkflowProject(IStructuredSelection selection)
  {
    WorkflowProject workflowProject = null;
    if (selection != null)
    {
      if (selection.getFirstElement() instanceof IProject)
      {
        workflowProject = getWorkflowProject((IProject)selection.getFirstElement());

      }
      else if (selection.getFirstElement() instanceof IJavaProject)
      {
        workflowProject = getWorkflowProject(((IJavaProject)selection.getFirstElement()).getProject());
      }
    }

    return workflowProject;
  }

  public WorkflowProject getRemoteWorkflowProject(IProject project)
  {
    return getRemoteWorkflowProject(project.getName());
  }

  public WorkflowProject getRemoteWorkflowProject(String projectName)
  {
    for (WorkflowProject workflowProject : getWorkflowProjects())
    {
      if (workflowProject.isRemote() && workflowProject.getName().equals(projectName))
        return workflowProject;
    }
    return null;
  }

  public List<WorkflowProject> getRemoteWorkflowProjects()
  {
    List<WorkflowProject> remoteProjects = new ArrayList<WorkflowProject>();
    for (WorkflowProject workflowProject : getWorkflowProjects())
    {
      if (workflowProject.isRemote())
        remoteProjects.add(workflowProject);
    }
    return remoteProjects;
  }

  /**
   * Get a workflow project based on the source project name
   */
  public WorkflowProject getWorkflowProject(String sourceProjectName)
  {
    for (WorkflowProject workflowProject : getWorkflowProjects())
    {
      if (workflowProject.getSourceProjectName().equals(sourceProjectName))
        return workflowProject;
    }
    return null;
  }

  /**
   * Get the workflow project whose source project corresponds to a specified java project
   */
  public WorkflowProject getWorkflowProject(IJavaProject javaProject) throws CoreException
  {
    return getWorkflowProject(javaProject.getProject().getName());
  }

  /**
   * Get the workflow faceted project matching the specified name
   */
  public IProject getWorkflowFacetedProject(String projectName)
  {
    for (IFacetedProject facetedProject : findWorkflowFacetedProjects())
    {
      if (facetedProject.getProject().getName().equals(projectName))
        return facetedProject.getProject();
    }
    return null;
  }

  public boolean isWorkflowProject(IProject project)
  {
    return getWorkflowFacetedProject(project.getName()) != null;
  }

  /**
   * Adds a workflow project and saves its settings.
   * @param workflowProject
   * @param earProject
   */
  public void add(WorkflowProject workflowProject, IProject earProject) throws CoreException
  {
    if (!getWorkflowProjects().contains(workflowProject))
    {
      getWorkflowProjects().add(workflowProject);
      fireProjectChangeEvent(workflowProject, ChangeType.ELEMENT_CREATE);
    }
    save(workflowProject, earProject);
  }

  public void save(WorkflowProject workflowProject) throws CoreException
  {
    IProject project = workflowProject.isEarProject() ? workflowProject.getEarProject() : workflowProject.getSourceProject();
    save(workflowProject, project);
  }

  /**
   * Persists a workflow project's settings in a workspace project.
   * @param workflowProject
   * @param project
   */
  public void save(WorkflowProject workflowProject, IProject project) throws CoreException
  {
    ProjectPersist projPersist = new ProjectPersist(workflowProject);
    if (workflowProject.isEarProject() && !hasEarFacet(project))
      project = workflowProject.getEarProject();  // project is Java project for EAR -- save should happen in EAR
    projPersist.write(project);
  }

  /**
   * Creates a workflow project based on a workflow-faceted project.
   */
  public WorkflowProject create(IProject project) throws CoreException
  {
    WorkflowProject workflowProject = new WorkflowProject();
    ProjectPersist projPersist = new ProjectPersist(workflowProject);
    return projPersist.read(project);
  }

  /**
   * Retrieves a workflow project from it's settings.
   */
  public WorkflowProject retrieve(WorkflowProject workflowProject) throws CoreException
  {
    ProjectPersist projPersist = new ProjectPersist(workflowProject);
    return projPersist.read((workflowProject.isRemote() || workflowProject.isCloudProject()) ? workflowProject.getSourceProject() : workflowProject.getEarProject());
  }

  /**
   * Convenience method to get a handle to a java project.
   */
  public static IJavaProject getJavaProject(String projectName)
  {
    return getJavaModel().getJavaProject(projectName);
  }

  public static List<IJavaProject> getJavaProjects() throws JavaModelException
  {
    return Arrays.asList(getJavaModel().getJavaProjects());
  }

  public static IProject getProject(String name)
  {
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
    if (project.exists())
      return project;
    else
      return null;
  }

  /**
   * Convenience method to get the java model.
   *
   * @return the java model
   */
  private static IJavaModel getJavaModel()
  {
    return JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
  }

  private List<WorkflowProject> findRemoteWorkflowProjects(List<IProject> excludes)
  {
    List<WorkflowProject> remoteProjects = new ArrayList<WorkflowProject>();

    // new-style loaded from project settings files
    for (IProject project : MdwPlugin.getWorkspaceRoot().getProjects())
    {
      // ignore closed, local and java projects
      if (project.isOpen() && getWorkflowFacetedProject(project.getName()) == null && !excludes.contains(project))
      {
        WorkflowProject remoteProject = null;
        try
        {
          remoteProject = new ProjectPersist(new WorkflowProject()).read(project);
        }
        catch (CoreException ex)
        {
          PluginMessages.log(ex);
        }
        if (remoteProject != null)
        {
          remoteProject.setRemote(true);
          remoteProjects.add(remoteProject);
        }
      }
    }

    return remoteProjects;
  }

  public static void updateProject(WorkflowProject workflowProject)
  {
    IProject project = MdwPlugin.getWorkspaceRoot().getProject(workflowProject.isEarProject() ? workflowProject.getEarProjectName() : workflowProject.getName());
    if (project == null)
    {
      PluginMessages.uiError("Project missing from workspace: " + workflowProject.getName(), "Update Project");
      return;
    }

    new ProjectPersist(workflowProject).write(project);
  }

  public static void registerProject(WorkflowProject workflowProject)
  {
    IProject project = MdwPlugin.getWorkspaceRoot().getProject(workflowProject.getName());
    if (project != null)
    {
      try
      {
        PluginUtil.createFolder(project, ".settings/", null);
      }
      catch(CoreException ex)
      {
        PluginMessages.log(ex);
        return;
      }

      new ProjectPersist(workflowProject).write(project);
    }
  }

  private static void deRegisterProject(WorkflowProject workflowProject)
  {
    // delete settings file for new style
    IProject project = MdwPlugin.getWorkspaceRoot().getProject(workflowProject.getName());
    if (project.exists())
    {
      IFile file = project.getFile(".settings/" + ProjectPersist.SETTINGS_FILE);
      if (file.exists())
      {
        try
        {
          file.delete(true, null);
        }
        catch (CoreException ex)
        {
          PluginMessages.log(ex);
        }
      }
    }
  }

  public static void addProject(WorkflowProject workflowProject)
  {
    if (!workflowProject.isRemote() && !workflowProject.isCloudProject())
      throw new RuntimeException("Method is for remote or cloud projects");

    registerProject(workflowProject);
    if (!getInstance().getWorkflowProjects().contains(workflowProject))
    {
      getInstance().getWorkflowProjects().add(workflowProject);
      getInstance().fireProjectChangeEvent(workflowProject, ChangeType.ELEMENT_CREATE);
    }
  }

  public static void deleteProject(WorkflowProject workflowProject)
  {
    if (!workflowProject.isRemote() && !workflowProject.isCloudProject())
      throw new RuntimeException("Can only delete remote or cloud projects");
    workflowProject.shutdownNoticeChecks();
    deRegisterProject(workflowProject);
    try
    {
      workflowProject.getSourceProject().delete(true, null);
    }
    catch (CoreException ex)
    {
      if (ex instanceof ResourceException && ex.getMessage().startsWith("Problems encountered while deleting"))
        MessageDialog.openWarning(MdwPlugin.getShell(), "Problems Deleting", "Not all resources under project '" + workflowProject.getName() + "' could be deleted.\nPlease delete the project manually on the file system.");
      else
        PluginMessages.uiError(ex, "Delete Project", workflowProject);
    }
    if (getInstance().getWorkflowProjects().contains(workflowProject))
    {
      getInstance().getWorkflowProjects().remove(workflowProject);
      getInstance().fireProjectChangeEvent(workflowProject, ChangeType.ELEMENT_DELETE);
    }
  }

  public static void renameRemoteProject(WorkflowProject workflowProject, String newName)
  {
    if (!workflowProject.isRemote())
      throw new RuntimeException("Can only rename remote projects");

    try
    {
      deRegisterProject(workflowProject);
      IProject workflowSourceProject = workflowProject.getSourceProject();
      if (workflowSourceProject != null && workflowSourceProject.exists())
      {
        RenameResourceProcessor renameProc = new RenameResourceProcessor(workflowSourceProject);
        renameProc.setNewResourceName(newName);
        RenameRefactoring refactoring = new RenameRefactoring(renameProc);
        refactoring.checkAllConditions(new NullProgressMonitor());
        Change change = refactoring.createChange(new NullProgressMonitor());
        change.perform(new NullProgressMonitor());
      }
      workflowProject.setSourceProjectName(newName);
      registerProject(workflowProject);
    }
    catch (CoreException ex)
    {
      // fail silently since older remote projects don't have physical counterparts
      PluginMessages.log(ex);
    }
  }

  public boolean projectNameExists(String workflowProjectName)
  {
    for (WorkflowProject workflowProject : getWorkflowProjects())
    {
      if (workflowProject.getName().equals(workflowProjectName))
        return true;
    }
    return false;
  }

  public List<WorkflowApplication> discoverWorkflowApps() throws DiscoveryException
  {
    String urlBase = MdwPlugin.getSettings().getDiscoveryUrl();
    if (!urlBase.endsWith("/"))
      urlBase += "/";
    String ctxRoot = urlBase.endsWith("Discovery/") ? "" : "MDWWeb/";
    if (urlBase.indexOf("lxdnd696") >= 0)
      ctxRoot = "MDWExampleWeb/";  // old discovery server
    String path = urlBase.endsWith("Discovery/") ? "ConfigManagerProjects.xml" : "Services/GetConfigFile?name=ConfigManagerProjects.xml";
    String cfgMgrUrl = urlBase + ctxRoot + path;
    try
    {
      URL url = new URL(cfgMgrUrl);
      HttpHelper httpHelper = new HttpHelper(url);
      String xml = httpHelper.get();
      ConfigManagerProjectsDocument doc = ConfigManagerProjectsDocument.Factory.parse(xml, Compatibility.namespaceOptions());
      return doc.getConfigManagerProjects().getWorkflowAppList();
    }
    catch (XmlException ex)
    {
      PluginMessages.log(ex);
      throw new DiscoveryException("Unable to obtain/parse Config Manager info from " + cfgMgrUrl);
    }
    catch (Exception ex)
    {
      throw new DiscoveryException(ex.getMessage(), ex);
    }
  }

  public List<ExtensionModule> getAvailableExtensions(WorkflowProject workflowProject)
  {
    List<ExtensionModule> available = new ArrayList<ExtensionModule>();
    for (ExtensionModule module : getAllExtensions())
    {
      if (module.select(workflowProject))
        available.add(module);
    }
    return available;
  }

  private List<ExtensionModule> allExtensions;
  private List<ExtensionModule> getAllExtensions()
  {
    if (allExtensions == null)
    {
      allExtensions = new ArrayList<ExtensionModule>();

      // read the plug-in extensions
      IExtensionRegistry registry = Platform.getExtensionRegistry();
      IExtensionPoint extensionPoint = registry.getExtensionPoint("com.centurylink.mdw.designer.ui.extensionModules");
      IExtension[] extensions = extensionPoint.getExtensions();
      for (IExtension extension : extensions)
      {
        for (IConfigurationElement element : extension.getConfigurationElements())
        {
          try
          {
            ExtensionModule module = (ExtensionModule) element.createExecutableExtension("class");
            module.setId(element.getAttribute("id"));
            module.setName(element.getAttribute("name"));
            module.setDescription(element.getAttribute("description"));
            module.setVersion(element.getAttribute("version"));  // null means MDW version
            String requiredMdwVersion = element.getAttribute("requiredMdwVersion");
            if (requiredMdwVersion != null)
              module.setRequiredMdwVersion(requiredMdwVersion); // null means any >= 5.1
            allExtensions.add(module);
          }
          catch (Exception ex)
          {
            PluginMessages.log("Unable to load MDW Extension for element: " + element);
            PluginMessages.log(ex);
          }
        }
      }
    }

    Collections.sort(allExtensions);

    return allExtensions;
  }

  /**
   * Listen for projects added and removed from the workspace
   */
  public void resourceChanged(IResourceChangeEvent event)
  {
    if (MdwPlugin.getPluginId() == null)
      return;  // during eclipse shutdown
    if (event.getType() == IResourceChangeEvent.POST_CHANGE)
    {
      if (event.getDelta().getResource() instanceof IWorkspaceRoot)
      {
        try
        {
          for (IResourceDelta childDelta : event.getDelta().getAffectedChildren())
          {
            if (childDelta.getResource() instanceof IProject)
            {
              IProject project = (IProject) childDelta.getResource();
              if (childDelta.getKind() == IResourceDelta.ADDED
                  || (childDelta.getKind() == IResourceDelta.CHANGED && (event.getDelta().getFlags() & IResourceDelta.OPEN) == 0 && project.isOpen()))
              {
                // project added or opened
                if (project.isOpen() && hasSettingsFile(project))
                {
                  WorkflowProject wfProject = loadWorkflowProject(project);
                  if (hasWorkflowFacet(project))
                  {
                    if (hasEarFacet(project))
                      wfProject.setEarProjectName(project.getName());
                    else
                      wfProject.setCloudProject(true);
                  }
                  else
                  {
                    wfProject.setRemote(true);
                  }

                  if (!getWorkflowProjects().contains(wfProject))
                  {
                    getWorkflowProjects().add(wfProject);
                    fireProjectChangeEvent(wfProject, ChangeType.ELEMENT_CREATE);
                  }
                }
              }
              else if (childDelta.getKind() == IResourceDelta.REMOVED
                  || (childDelta.getKind() == IResourceDelta.CHANGED && (event.getDelta().getFlags() & IResourceDelta.OPEN) == 0 && !project.isOpen()))
              {
                // project deleted or closed
                WorkflowProject wfProject = getWorkflowProject(project);
                if (getWorkflowProjects().contains(wfProject))
                {
                  getWorkflowProjects().remove(wfProject);
                  fireProjectChangeEvent(wfProject, ChangeType.ELEMENT_DELETE);
                }
              }
            }
          }
        }
        catch (CoreException ex)
        {
          PluginMessages.log(ex);
        }
      }
    }
  }

  public boolean hasSettingsFile(IProject project)
  {
    return project.getFile(".settings/" + ProjectPersist.SETTINGS_FILE).exists()
        || project.getFile(".settings/" + ProjectPersist.LEGACY_SETTINGS_FILE).exists();
  }

  public boolean hasWorkflowFacet(IProject project) throws CoreException
  {
    for (IFacetedProject facetedProject : ProjectFacetsManager.getFacetedProjects())
    {
      if (facetedProject.getProject().equals(project))
      {
        for (IProjectFacetVersion projectFacetVersion : facetedProject.getProjectFacets())
        {
          IProjectFacet projectFacet = projectFacetVersion.getProjectFacet();
          if (projectFacet.getId().equals("mdw.workflow"))
            return true;
        }
      }
    }
    return false;
  }

  public boolean hasEarFacet(IProject project) throws CoreException
  {
    for (IFacetedProject facetedProject : ProjectFacetsManager.getFacetedProjects())
    {
      if (facetedProject.getProject().equals(project))
      {
        for (IProjectFacetVersion projectFacetVersion : facetedProject.getProjectFacets())
        {
          IProjectFacet projectFacet = projectFacetVersion.getProjectFacet();
          if (projectFacet.getId().equals("jst.ear"))
            return true;
        }
      }
    }
    return false;
  }

  public IJavaProject getRelatedJavaProject(IProject earProject)
  {
    try
    {
      if (!hasEarFacet(earProject))
        return null;
      for (IProject project : earProject.getDescription().getReferencedProjects())
      {
        IJavaProject javaProject = getJavaProject(project.getName());
        if (javaProject != null)
          return javaProject;
      }
      return null;
    }
    catch (CoreException ex)
    {
      PluginMessages.log(ex);
      return null;
    }
  }

  private List<ElementChangeListener> elementChangeListeners = new Vector<ElementChangeListener>();
  public List<ElementChangeListener> getElementChangeListeners() { return elementChangeListeners; }
  public void addElementChangeListener(ElementChangeListener listener)
  {
    elementChangeListeners.add(listener);
  }
  public void removeElementChangeListener(ElementChangeListener listener)
  {
    elementChangeListeners.remove(listener);
  }
  public void fireProjectChangeEvent(WorkflowProject workflowProject, ChangeType changeType)
  {
    for (ElementChangeListener listener : getElementChangeListeners())
    {
      ElementChangeEvent ece = new ElementChangeEvent(changeType, workflowProject);
      ece.setNewValue(workflowProject);
      listener.elementChanged(ece);
    }
  }

  /**
   * Global authenticated users by Authenticator impl class/key name;
   */
  private Map<String,User> authenticatedUsers = new HashMap<String,User>();

  /**
   * Triggers automatic authentication if credentials are in Eclipse secure store.
   */
  public User getAuthenticatedUser(Authenticator authenticator)
  {
    String key = authenticator.getClass().getName() + "_" + authenticator.getKey();
    User authUser = authenticatedUsers.get(key);

    if (authUser == null)
    {
      try
      {
        ISecurePreferences securePrefs = SecurePreferencesFactory.getDefault();
        String user = securePrefs.get(PreferenceConstants.PREFS_MDW_USER + "_" + key, "");
        if (user.length() > 0)
        {
          String password = securePrefs.get(PreferenceConstants.PREFS_MDW_PASSWORD + "_" + key, "");
          if (password.length() > 0)
          {
            try
            {
              authenticate(authenticator, user, password, false);
              authUser = new User(user, password);
            }
            catch (MdwSecurityException ex)
            {
              // prevent repeated attempts to auto-authenticate
              securePrefs.put(PreferenceConstants.PREFS_MDW_USER + "_" + key, "", false);
              securePrefs.flush();
            }
          }
        }
      }
      catch (Exception ex)
      {
        // just log exception and force user to log in -- if pw expired they'll enter the new one
        PluginMessages.log(ex);
      }
    }
    return authUser;
  }

  /**
   * Authenticates using the designated authenticator impl.
   */
  public void authenticate(Authenticator authenticator, String user, String password, boolean saveInSecureStore) throws MdwSecurityException
  {
    String key = authenticator.getClass().getName() + "_" + authenticator.getKey();
    authenticatedUsers.remove(key);
    try
    {
      authenticator.authenticate(user, password);
      if (saveInSecureStore)
      {
        try
        {
          ISecurePreferences securePrefs = SecurePreferencesFactory.getDefault();
          securePrefs.put(PreferenceConstants.PREFS_MDW_USER + "_" + key, user, false);
          securePrefs.put(PreferenceConstants.PREFS_MDW_PASSWORD + "_" + key, password, true);
          securePrefs.flush();
        }
        catch (Exception ex)
        {
          // don't prevent user from being authenticated because of this
          PluginMessages.log(ex);
        }
      }
      authenticatedUsers.put(key, new User(user, password));
    }
    catch (MdwSecurityException ex)
    {
      PluginMessages.log(ex);
      throw ex;
    }
    catch (Exception ex)
    {
      PluginMessages.log(ex);
      throw new MdwSecurityException(ex.getMessage(), ex);
    }
  }
}
