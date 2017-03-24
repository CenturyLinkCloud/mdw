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
package com.centurylink.mdw.plugin.designer.storage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;

public class JavaSourceStorage extends WorkflowElement implements IStorage {
    private String className;

    public String getClassName() {
        return className;
    }

    private String sourceCode;

    public String getSourceCode() {
        return sourceCode;
    }

    public JavaSourceStorage(WorkflowProject workflowProj, String className, String sourceCode) {
        setProject(workflowProj);
        this.className = className;
        this.sourceCode = sourceCode;
    }

    public InputStream getContents() throws CoreException {
        return new ByteArrayInputStream(sourceCode.getBytes());
    }

    public String getName() {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    public IPath getFullPath() {
        return null;
    }

    public boolean isReadOnly() {
        return true;
    }

    public boolean isUserAllowedToEdit() {
        return false;
    }

    @SuppressWarnings("rawtypes")
    public Object getAdapter(Class adapter) {
        return null;
    }

    @Override
    public String getIcon() {
        return "java.gif";
    }

    public Entity getActionEntity() {
        return Entity.File;
    }

    @Override
    public Long getId() {
        return null;
    }

    @Override
    public String getTitle() {
        return getClassName();
    }

    @Override
    public boolean hasInstanceInfo() {
        return false;
    }

}
