/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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
