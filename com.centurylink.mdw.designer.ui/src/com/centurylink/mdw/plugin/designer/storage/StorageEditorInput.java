/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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
