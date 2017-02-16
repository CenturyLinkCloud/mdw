/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.editors;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.wst.xml.ui.internal.tabletree.IDesignViewer;
import org.eclipse.wst.xml.ui.internal.tabletree.XMLMultiPageEditorPart;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.model.ViewLayout;

@SuppressWarnings("restriction")
public class ViewLayoutXmlEditor extends XMLMultiPageEditorPart {
    private FileEditorInput editorInput;

    public ViewLayoutXmlEditor() {
        noToolbar();
    }

    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
        this.editorInput = (FileEditorInput) input;
    }

    @Override
    protected IDesignViewer createDesignPage() {
        Composite container = getDesignContainer(getContainer());

        ViewLayoutTableTreeViewer tableTreeViewer = new ViewLayoutTableTreeViewer(container,
                new ViewLayout(editorInput.getFile()));
        tableTreeViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        MdwPlugin.getPluginWorkbench().getHelpSystem().setHelp(tableTreeViewer.getControl(),
                MdwPlugin.getPluginId() + ".view_layout_editor_help");

        return tableTreeViewer;
    }
}
