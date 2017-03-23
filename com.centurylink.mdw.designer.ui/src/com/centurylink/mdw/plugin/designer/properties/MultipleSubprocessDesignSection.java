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
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.properties.editor.AssetLocator;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor.DefaultRowImpl;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

public class MultipleSubprocessDesignSection extends DesignSection implements IFilter {
    @Override
    protected void preRender(final PropertyEditor propertyEditor) {
        if (WorkAttributeConstant.PROCESS_MAP.equals(propertyEditor.getName())) {
            propertyEditor.addValueChangeListener(new ValueChangeListener() {
                public void propertyValueChanged(Object newValue) {
                    DefaultRowImpl row = (DefaultRowImpl) newValue;
                    String procName = row.getColumnValues()[2];
                    String ver = row.getColumnValues()[3];
                    if (procName != null && ver != null) {
                        AssetVersionSpec spec = new AssetVersionSpec(procName, ver);
                        AssetLocator locator = new AssetLocator(getActivity(),
                                AssetLocator.Type.Process);
                        WorkflowProcess subproc = locator.getProcessVersion(spec);
                        if (subproc != null)
                            openSubprocess(subproc);
                    }
                }
            });
        }
    }

    @Override
    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof Activity))
            return false;

        Activity activity = (Activity) toTest;

        if (activity.isForProcessInstance())
            return false;

        return activity.isHeterogeneousSubProcInvoke();
    }

    private void openSubprocess(WorkflowProcess subproc) {
        IWorkbenchPage page = MdwPlugin.getActivePage();
        try {
            page.openEditor(subproc, "mdw.editors.process");
        }
        catch (PartInitException ex) {
            PluginMessages.uiError(ex, "Open Process", subproc.getProject());
        }
    }
}
