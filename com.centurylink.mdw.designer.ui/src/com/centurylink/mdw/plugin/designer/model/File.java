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
package com.centurylink.mdw.plugin.designer.model;

import java.net.URL;

import org.eclipse.core.resources.IFile;

import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * Wraps a workspace file.
 */
public class File extends WorkflowElement implements Comparable<File> {
    private IFile workspaceFile;

    public IFile getWorkspaceFile() {
        return workspaceFile;
    }

    private WorkflowElement parent;

    public WorkflowElement getParent() {
        return parent;
    }

    void setParent(WorkflowElement parent) {
        this.parent = parent;
    }

    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    private String name;

    public String getName() {
        return workspaceFile == null ? name : workspaceFile.getName();
    }

    private URL url;

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    private String icon = "file.gif";

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public Entity getActionEntity() {
        return Entity.File;
    }

    public File(WorkflowProject workflowProject, WorkflowElement parent, IFile workspaceFile) {
        setProject(workflowProject);
        this.parent = parent;
        this.workspaceFile = workspaceFile;
    }

    public File(Folder parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public String getTitle() {
        return "File";
    }

    @Override
    public Long getId() {
        return convertToLong(getName());
    }

    public boolean isReadOnly() {
        return false;
    }

    public boolean hasInstanceInfo() {
        return false;
    }

    public int compareTo(File other) {
        return getName().compareTo(other.getName());
    }

    public boolean equals(Object other) {
        if (other instanceof File && getProject() == null && ((File) other).getProject() == null)
            return getPath().equals(((File) other).getPath());
        else
            return super.equals(other);
    }

    public String getPath() {
        if (getParent() != null)
            return getParent().getName() + "/" + getName();
        else
            return getName();
    }

    public boolean isLocal() {
        return true;
    }

}
