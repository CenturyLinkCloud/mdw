/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.editors;

import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;

import com.centurylink.mdw.plugin.designer.DirtyStateListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.TabbedPropertySheetPage;

public abstract class WorkflowElementEditor extends EditorPart implements
        ITabbedPropertySheetPageContributor, DirtyStateListener, ISaveablePart, ISaveablePart2 {
    public abstract WorkflowElement getElement();

    public abstract void notifyNameChange();

    private boolean dirty;

    public boolean isDirty() {
        if (isReadOnly())
            return false;

        return dirty;
    }

    private boolean visible;

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isReadOnly() {
        return getElement().isReadOnly();
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void doSaveAs() {
        // not allowed
    }

    public String getContributorId() {
        return "mdw.tabbedprops.contributor"; // see plugin.xml
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object getAdapter(Class type) {
        if (type == IPropertySheetPage.class)
            return new TabbedPropertySheetPage(this);

        return super.getAdapter(type);
    }

    public void dirtyStateChanged(boolean dirty) {
        this.dirty = dirty;
        firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
        firePropertyChange(IWorkbenchPartConstants.PROP_PART_NAME);
    }
}
