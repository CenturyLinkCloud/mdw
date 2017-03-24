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

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;

public class StorageEditorInput implements IStorageEditorInput {
    private IStorage storage;

    public IStorage getStorage() {
        return storage;
    }

    public WorkflowElement getElement() {
        if (storage == null)
            return null;

        return (WorkflowElement) storage;
    }

    public StorageEditorInput(IStorage storage) {
        this.storage = storage;
    }

    public boolean exists() {
        return storage != null;
    }

    private ImageDescriptor imageDesc = null;

    public ImageDescriptor getImageDescriptor() {
        WorkflowElement workflowElement = getElement();
        if (workflowElement == null)
            return null;

        if (imageDesc == null)
            imageDesc = MdwPlugin.getImageDescriptor("icons/" + workflowElement.getIcon());

        return imageDesc;
    }

    public String getName() {
        if (getElement() == null)
            return null;

        return getElement().getName();
    }

    public IPersistableElement getPersistable() {
        return null;
    }

    public String getToolTipText() {
        if (getElement() == null)
            return null;

        return getElement().getTitle();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object getAdapter(Class adapter) {
        return null;
    }

}
