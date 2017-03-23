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
package com.centurylink.mdw.plugin.designer.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.ResourceWrapper;

public class FolderTreeDialog extends ElementTreeSelectionDialog {
    public FolderTreeDialog(Shell parent) {
        super(parent, new WorkbenchLabelProvider(), new WorkbenchContentProvider() {
            public Object[] getChildren(Object element) {
                List<Object> children = new ArrayList<Object>();
                Object[] all = super.getChildren(element);
                for (Object obj : all) {
                    ResourceWrapper resourceWrapper = new ResourceWrapper(obj);
                    try {
                        IFolder folder = resourceWrapper.getFolder();
                        if (folder != null)
                            children.add(obj);
                    }
                    catch (JavaModelException ex) {
                        PluginMessages.log(ex);
                    }
                }
                return children.toArray();
            }
        });
    }
}
