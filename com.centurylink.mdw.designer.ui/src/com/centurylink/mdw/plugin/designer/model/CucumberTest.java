/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.centurylink.mdw.plugin.ResourceWrapper;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class CucumberTest
{
  private IProject project;
  public IProject getProject() { return project; }

  private IFile file;
  public IFile getFile() { return file; }

  public CucumberTest(IProject project, IFile file)
  {
    this.project = project;
    this.file = file;
  }

  public String getName()
  {
    return file.getName();
  }

  public String getPath()
  {
    return file.getProjectRelativePath().toString();
  }

  public static List<CucumberTest> getTests(IProject project) throws CoreException
  {
    List<CucumberTest> tests = new ArrayList<CucumberTest>();
    for (IResource res : project.members(false))
    {
      ResourceWrapper resourceWrapper = new ResourceWrapper(res);
      IFolder folder = resourceWrapper.getFolder();
      if (folder != null)
        findTests(folder, tests);
    }

    return tests;
  }

  public static List<CucumberTest> getTests(IFolder folder) throws CoreException
  {
    List<CucumberTest> tests = new ArrayList<CucumberTest>();
    findTests(folder, tests);
    return tests;
  }

  public static void findTests(IFolder folder, List<CucumberTest> addTo) throws CoreException
  {
    ResourceWrapper folderWrapper = new ResourceWrapper(folder);
    WorkflowProject workflowProject = folderWrapper.getOwningWorkflowProject();
    // include only non-workflow package folders
    if ((workflowProject == null || workflowProject.getPackage(folder) == null) && !"testCases".equals(folder.getProjectRelativePath().toString()))
    {
      for (IResource res : folder.members(false))
      {
        ResourceWrapper resourceWrapper = new ResourceWrapper(res);
        IFile file = resourceWrapper.getFile();
        if (file != null)
        {
          if (file.exists() && !file.isDerived() && "feature".equals(file.getFileExtension()))
            addTo.add(new CucumberTest(file.getProject(), file));
        }
        else
        {
          IFolder subFolder = resourceWrapper.getFolder();
          if (subFolder != null && !subFolder.isDerived())
            findTests(subFolder, addTo);
        }
      }
    }
  }

}
