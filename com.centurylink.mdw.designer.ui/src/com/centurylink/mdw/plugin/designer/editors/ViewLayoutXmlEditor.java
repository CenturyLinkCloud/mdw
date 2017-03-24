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
