/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

public class ClasspathComputer
{
  private IJavaProject javaProject;
  public ClasspathComputer(IJavaProject javaProject)
  {
    this.javaProject = javaProject;
  }

  private List<String> classpathList;
  public List<String> getClasspathList() throws CoreException
  {
    classpathList = new ArrayList<String>();
    addSourceProjectPaths(javaProject);
    return classpathList;
  }

  private void addSourceProjectPaths(IJavaProject javaProject) throws CoreException
  {
    IPath outputLoc = javaProject.getOutputLocation();
    IResource outputRes = ResourcesPlugin.getWorkspace().getRoot().findMember(outputLoc);
    if (outputRes != null)
    classpathList.add(outputRes.getRawLocation().toString());
    for (IClasspathEntry classpathEntry: javaProject.getResolvedClasspath(true))
    {
      if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE)
      {
        if (classpathEntry.getOutputLocation() != null)
        {
          outputRes = ResourcesPlugin.getWorkspace().getRoot().findMember(classpathEntry.getOutputLocation());
          if (outputRes != null)
            classpathList.add(ResourcesPlugin.getWorkspace().getRoot().findMember(classpathEntry.getOutputLocation()).getRawLocation().toString());
        }
      }
      else if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT)
      {
        IJavaProject depProject = WorkflowProjectManager.getJavaProject(classpathEntry.getPath().lastSegment());
        if (depProject != null && depProject.exists())
          addSourceProjectPaths(depProject);
      }
      else
      {
        IResource workspaceRes = ResourcesPlugin.getWorkspace().getRoot().findMember(classpathEntry.getPath());
        if (workspaceRes != null)
          classpathList.add(workspaceRes.getRawLocation().toString());
        else
          classpathList.add(classpathEntry.getPath().toString());
      }
    }
  }
}
