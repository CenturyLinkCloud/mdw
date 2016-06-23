/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * Utility for obtaining a desired resource type from an IAdaptable or Object.
 */
public class ResourceWrapper
{
  private IAdaptable adaptable;
  private Object resourceObj;

  public ResourceWrapper(IAdaptable adaptable)
  {
    this.adaptable = adaptable;
  }

  public ResourceWrapper(Object resourceObj)
  {
    this.resourceObj = resourceObj;
  }

  public IFile getFile()
  {
    IFile file = null;
    if (adaptable != null)
      file = (IFile)adaptable.getAdapter(IFile.class);
    else
      file = (resourceObj instanceof IFile) ? (IFile)resourceObj : null;

    return file;
  }

  public IFolder getFolder() throws JavaModelException
  {
    IFolder folder = null;
    if (adaptable != null)
    {
      folder = (IFolder)adaptable.getAdapter(IFolder.class);
      if (folder == null)
      {
        // try as java element
        IPackageFragmentRoot pkgFragmentRoot = (IPackageFragmentRoot)adaptable.getAdapter(IPackageFragmentRoot.class);
        if (pkgFragmentRoot != null)
        {
          IResource res = pkgFragmentRoot.getCorrespondingResource();
          folder = (IFolder)res.getAdapter(IFolder.class);
        }
        else
        {
          IPackageFragment pkgFragment = (IPackageFragment)adaptable.getAdapter(IPackageFragment.class);
          if (pkgFragment != null)
          {
            IResource res = pkgFragment.getCorrespondingResource();
            folder = (IFolder)res.getAdapter(IFolder.class);
          }
        }
      }
    }
    else
    {
      if (resourceObj instanceof IFolder)
      {
        folder = (IFolder) resourceObj;
      }
      else if (resourceObj instanceof IPackageFragmentRoot)
      {
        IPackageFragmentRoot pkgFragmentRoot = (IPackageFragmentRoot) resourceObj;
        IResource res = pkgFragmentRoot.getCorrespondingResource();
        folder = (IFolder)res.getAdapter(IFolder.class);
      }
      else if (resourceObj instanceof IPackageFragment)
      {
        IPackageFragment pkgFragment = (IPackageFragment) resourceObj;
        IResource res = pkgFragment.getCorrespondingResource();
        folder = (IFolder)res.getAdapter(IFolder.class);
      }
    }
    return folder;
  }

  public IProject getProject()
  {
    IProject project = null;
    if (adaptable != null)
    {
      project = (IProject)adaptable.getAdapter(IProject.class);
      if (project == null)
      {
        IJavaProject javaProj = (IJavaProject)adaptable.getAdapter(IJavaProject.class);
        if (javaProj != null)
          project = javaProj.getProject();
      }
    }
    else
    {
      if (resourceObj instanceof IProject)
        project = (IProject) resourceObj;
      else if (resourceObj instanceof IJavaProject)
        project = ((IJavaProject)resourceObj).getProject();
    }
    return project;
  }

  public IJavaProject getJavaProject() throws JavaModelException
  {
    IJavaProject javaProject = null;
    IProject project = getProject();
    if (project != null)
    {
      IJavaModel javaModel = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
      if (javaModel != null)
        javaProject = javaModel.getJavaProject(project.getName());
    }
    return javaProject;
  }

  public WorkflowProject getWorkflowProject()
  {
    WorkflowProject workflowProject = null;
    IProject project = getProject();
    if (project != null)
      workflowProject = WorkflowProjectManager.getInstance().getWorkflowProject(project);
    return workflowProject;
  }

  public IProject getOwningProject() throws JavaModelException
  {
    IProject project = getProject();
    if (project == null)
    {
      IFolder folder = getFolder();
      if (folder != null)
      {
        project = folder.getProject();
      }
      else
      {
        IFile file = getFile();
        if (file != null)
          project = file.getProject();
      }
    }
    return project;
  }

  public IJavaProject getOwningJavaProject() throws JavaModelException
  {
    IJavaProject javaProject = null;
    IProject owningProject = getOwningProject();
    if (owningProject != null)
    {
      IJavaModel javaModel = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
      if (javaModel != null)
        javaProject = javaModel.getJavaProject(owningProject.getName());
    }
    return javaProject;
  }

  public WorkflowProject getOwningWorkflowProject() throws JavaModelException
  {
    WorkflowProject owningWorkflowProject = null;
    IProject owningProject = getOwningProject();
    if (owningProject != null)
      owningWorkflowProject = WorkflowProjectManager.getInstance().getWorkflowProject(owningProject);
    return owningWorkflowProject;
  }

  /**
   * Returns all subfolder members recursively.
   * Returns null if resource cannot be converted to an IContainer.
   */
  public List<IFolder> getAllFolders() throws CoreException
  {
    IContainer container = null;
    if (adaptable != null)
    {
      container = (IContainer)adaptable.getAdapter(IContainer.class);
    }
    else
    {
      if (resourceObj instanceof IContainer)
        container = (IContainer) resourceObj;
    }

    if (container == null) // try as folder (for package fragments)
    {
      IFolder folder = getFolder();
      if (folder != null)
        container = folder;
    }

    if (container == null)
    {
      return null;
    }
    else
    {
      List<IFolder> folders = new ArrayList<IFolder>();
      findFolders(container, folders);
      return folders;
    }
  }

  private void findFolders(IContainer container, List<IFolder> addTo) throws CoreException
  {
    for (IResource res : container.members(false))
    {
      IFolder subFolder = (IFolder)res.getAdapter(IFolder.class);
      if (subFolder == null)
      {
        ResourceWrapper subWrapper = new ResourceWrapper(res);
        subFolder = subWrapper.getFolder();
      }
      if (subFolder != null)
      {
        addTo.add(subFolder);
        findFolders(subFolder, addTo);
      }
    }
  }
}
