/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.plugin.codegen.meta;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public abstract class Code {
    // source folder
    private IPackageFragmentRoot packageFragmentRoot;

    public IPackageFragmentRoot getPackageFragmentRoot() {
        return packageFragmentRoot;
    }

    public void setPackageFragmentRoot(IPackageFragmentRoot pfr) {
        packageFragmentRoot = pfr;
    }

    // package
    private IPackageFragment packageFragment;

    public IPackageFragment getPackageFragment() {
        return packageFragment;
    }

    public void setPackageFragment(IPackageFragment pf) {
        packageFragment = pf;
    }

    private String javaPackage;

    public String getJavaPackage() {
        return javaPackage;
    }

    public void setJavaPackage(String javaPackage) {
        this.javaPackage = javaPackage;
    }

    public String getJavaPackagePath() {
        return PluginUtil.convertPackageToPath(getJavaPackage());
    }

    // class
    private String className;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    // workflow project
    private WorkflowProject project;

    public WorkflowProject getProject() {
        return project;
    }

    public void setProject(WorkflowProject project) {
        this.project = project;
    }

    // workflow package
    protected WorkflowPackage workflowPackage;

    public WorkflowPackage getPackage() {
        return workflowPackage;
    }

    public void setPackage(WorkflowPackage workflowPackage) {
        this.workflowPackage = workflowPackage;
    }

    public void setFullyQualifiedClassName(String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot == -1) {
            javaPackage = null;
            this.className = className;
        }
        else {
            javaPackage = className.substring(0, lastDot);
            this.className = className.substring(lastDot + 1);
        }
    }

    // author
    private String author;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String toString() {
        return "workflowProject: " + getProject() + "\n" + "workflowPackage: " + getPackage() + "\n"
                + "packageName: " + getJavaPackage() + "\n" + "className: " + getClassName() + "\n"
                + "author: " + getAuthor() + "\n";
    }
}
