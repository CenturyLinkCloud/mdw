/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.codegen.meta;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public abstract class Code
{
  // source folder
  private IPackageFragmentRoot packageFragmentRoot;
  public IPackageFragmentRoot getPackageFragmentRoot() { return packageFragmentRoot; }
  public void setPackageFragmentRoot(IPackageFragmentRoot pfr) { packageFragmentRoot = pfr; }

  // package
  private IPackageFragment packageFragment;
  public IPackageFragment getPackageFragment() { return packageFragment; }
  public void setPackageFragment(IPackageFragment pf) { packageFragment = pf; }

  private String javaPackage;
  public String getJavaPackage() { return javaPackage; }
  public void setJavaPackage(String javaPackage) { this.javaPackage = javaPackage; }

  public String getJavaPackagePath()
  {
    return PluginUtil.convertPackageToPath(getJavaPackage());
  }

  // class
  private String className;
  public String getClassName() { return className; }
  public void setClassName(String className) { this.className = className; }

  // workflow project
  private WorkflowProject project;
  public WorkflowProject getProject() { return project; }
  public void setProject(WorkflowProject project) { this.project = project; }

  // workflow package
  protected WorkflowPackage workflowPackage;
  public WorkflowPackage getPackage() { return workflowPackage; }
  public void setPackage(WorkflowPackage workflowPackage) { this.workflowPackage = workflowPackage; }

  public void setFullyQualifiedClassName(String className)
  {
    int lastDot = className.lastIndexOf('.');
    if (lastDot == -1)
    {
      javaPackage = null;
      this.className = className;
    }
    else
    {
      javaPackage = className.substring(0, lastDot);
      this.className = className.substring(lastDot + 1);
    }
  }

  // author
  private String author;
  public String getAuthor() { return author; }
  public void setAuthor(String author) { this.author = author; }

  public String toString()
  {
    return "workflowProject: " + getProject() + "\n"
      + "workflowPackage: " + getPackage() + "\n"
      + "packageName: " + getJavaPackage() + "\n"
      + "className: " + getClassName() + "\n"
      + "author: " + getAuthor() + "\n";
  }
}
