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
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.DocumentVO;

public class DocumentStorage extends WorkflowElement implements IStorage {
    private String name;
    private String contents;
    private DocumentReference docRef;

    public DocumentReference getDocRef() {
        return docRef;
    }

    public DocumentStorage(WorkflowProject workflowProj, String name, String contents) {
        setProject(workflowProj);
        this.name = name;
        this.contents = contents;
    }

    public DocumentStorage(WorkflowProject workflowProj, DocumentReference docRef) {
        setProject(workflowProj);
        this.docRef = docRef;
    }

    public InputStream getContents() throws CoreException {
        if (contents != null) {
            return new ByteArrayInputStream(contents.getBytes());
        }
        else {
            DocumentVO document = getProject().getDesignerProxy().getDocument(docRef);
            return new ByteArrayInputStream(document.getContent().getBytes());
        }
    }

    public String getName() {
        if (name != null) {
            if (contents != null && contents.trim().startsWith("{"))
                return name + ".json";
            else
                return name + ".xml";
        }
        else {
            return docRef.toString();
        }
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
        return "doc.gif";
    }

    public Entity getActionEntity() {
        return Entity.Document;
    }

    @Override
    public Long getId() {
        return null;
    }

    @Override
    public String getTitle() {
        return getName();
    }

    @Override
    public boolean hasInstanceInfo() {
        return false;
    }

}
