/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.ResourceWrapper;
import com.centurylink.mdw.plugin.designer.model.CucumberTest;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class AutoTestPropertyTester extends PropertyTester
{
  // TODO: support multiple selections
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue)
  {
    ResourceWrapper resourceWrapper = new ResourceWrapper((IAdaptable)receiver);
    try
    {
      WorkflowProject workflowProject = resourceWrapper.getOwningWorkflowProject();
      if ("canLaunchAutomatedTest".equals(property))
      {
        IFile file = resourceWrapper.getFile();
        if (file != null)
        {
          if (("test".equals(file.getFileExtension()) || "feature".equals(file.getFileExtension())) &&
              file.exists() && !file.isDerived())
          {
            if (workflowProject != null)
            {
              WorkflowPackage pkg = workflowProject.getPackage((IFolder)file.getParent());
              if (pkg != null)
                return pkg.getAsset(file) != null;
            }
          }
        }
      }
      else if ("canLaunchAutomatedTests".equals(property))
      {
        IFolder folder = resourceWrapper.getFolder();
        if (folder != null)
        {
          workflowProject = WorkflowProjectManager.getInstance().getWorkflowProject(folder.getProject());
          if (workflowProject != null)
          {
            WorkflowPackage pkg = workflowProject.getPackage(folder);
            if (pkg != null)
              return !pkg.getTestCases().isEmpty();
          }
        }
        else
        {
          IProject project = resourceWrapper.getProject();
          if (project != null)
          {
            WorkflowProject proj = WorkflowProjectManager.getInstance().getWorkflowProject(project);
            if (proj != null)
              return !proj.getTestCases().isEmpty();
          }
        }
      }
      else if ("canLaunchCucumberTest".equals(property))
      {
        if (resourceWrapper.getOwningJavaProject() != null)
        {
          IFile file = resourceWrapper.getFile();
          if (file != null && "feature".equals(file.getFileExtension()) && file.exists() && !file.isDerived())
          {
            // exclude mdw automated gherking tests
            if (workflowProject != null)
            {
              ResourceWrapper parentWrapper = new ResourceWrapper((IAdaptable)file.getParent());
              IFolder folder = parentWrapper.getFolder();
              if (folder != null && workflowProject.getPackage(folder) != null)
                return false;
            }
            return true;
          }
        }
      }
      else if ("canLaunchCucumberTests".equals(property))
      {
        if (resourceWrapper.getOwningJavaProject() != null)
        {
          IFolder folder = resourceWrapper.getFolder();
          if (folder != null)
          {
            if (workflowProject != null && workflowProject.getPackage(folder) != null)
              return false;
            List<CucumberTest> tests = new ArrayList<CucumberTest>();
            CucumberTest.findTests(folder, tests);
            return !tests.isEmpty();
          }
          else
          {
            IProject project = resourceWrapper.getProject();
            if (project != null)
              return !CucumberTest.getTests(project).isEmpty();
          }
        }
      }
    }
    catch (CoreException ex)
    {
      PluginMessages.log(ex);
    }

    return false;
  }
}
