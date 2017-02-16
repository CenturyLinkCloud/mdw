/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;

import com.centurylink.mdw.plugin.designer.editors.ProcessEditor;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;

/**
 * Extends Eclipse TabbedPropertySheetPage to allow access to currentSelection
 * for dynamic property sheet widget layout.
 */
public class TabbedPropertySheetPage
        extends org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage {
    private ISelection currentSelection;

    public ISelection getCurrentSelection() {
        return currentSelection;
    }

    public TabbedPropertySheetPage(
            ITabbedPropertySheetPageContributor tabbedPropertySheetPageContributor) {
        super(tabbedPropertySheetPageContributor);
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (part instanceof ProcessEditor && selection.equals(currentSelection))
            return;

        if (selection instanceof WorkflowProcess && selection.equals(currentSelection)) {
            // fool super into thinking the selection changed
            WorkflowProcess throwaway = new WorkflowProcess((WorkflowProcess) selection);
            throwaway.setDummy(true);
            super.selectionChanged(part, throwaway);
        }

        this.currentSelection = selection;
        if (selection instanceof WorkflowProcess) {
            WorkflowProcess processVersion = (WorkflowProcess) selection;
            if (getSite().getPage().findEditor(processVersion) == null)
                processVersion.setReadOnly(true);
        }

        super.selectionChanged(part, selection);
    }
}
