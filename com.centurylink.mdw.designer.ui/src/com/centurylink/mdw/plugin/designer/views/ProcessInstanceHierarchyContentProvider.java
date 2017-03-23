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
package com.centurylink.mdw.plugin.designer.views;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

import com.centurylink.mdw.model.value.process.LinkedProcessInstance;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;

public class ProcessInstanceHierarchyContentProvider
        implements ITreeContentProvider, ElementChangeListener {
    private ProcessInstanceVO processInstance;
    private LinkedProcessInstance topInstance;
    private LinkedProcessInstance startInstance;

    public Object[] getElements(Object inputElement) {
        if (inputElement == null)
            return new Object[0];
        if (!(inputElement instanceof WorkflowProcess))
            throw new IllegalArgumentException("Invalid object not instance of WorkflowProcess");
        WorkflowProcess procVer = (WorkflowProcess) inputElement;
        if (!procVer.hasInstanceInfo())
            return new Object[0]; // not relevant (definition hierarchy)

        if (topInstance == null || !procVer.getProcessInstance().equals(processInstance)) {
            processInstance = procVer.getProcessInstance();
            try {
                topInstance = procVer.getProject().getDesignerProxy()
                        .getProcessInstanceCallHierarchy(processInstance);
                if (topInstance.getProcessInstance().equals(processInstance))
                    startInstance = topInstance;
                else
                    startInstance = findStartInstance(topInstance);
            }
            catch (Exception ex) {
                PluginMessages.uiError(ex, "Process Instance Hierarchy", procVer.getProject());
                return new LinkedProcessInstance[0];
            }
        }

        return new LinkedProcessInstance[] { topInstance };
    }

    private LinkedProcessInstance findStartInstance(LinkedProcessInstance caller) {
        for (LinkedProcessInstance called : caller.getChildren()) {
            if (called.getProcessInstance().equals(processInstance)) {
                return called;
            }
            else {
                LinkedProcessInstance found = findStartInstance(called);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    public Object[] getChildren(Object parentElement) {
        if (!(parentElement instanceof LinkedProcessInstance))
            return null;

        return ((LinkedProcessInstance) parentElement).getChildren()
                .toArray(new LinkedProcessInstance[0]);
    }

    public boolean hasChildren(Object element) {
        if (!(element instanceof LinkedProcessInstance))
            return false;

        return ((LinkedProcessInstance) element).getChildren().size() > 0;
    }

    public Object getParent(Object element) {
        if (!(element instanceof LinkedProcessInstance))
            return null;

        return ((LinkedProcessInstance) element).getParent();
    }

    public ISelection getInitialSelection() {
        return new StructuredSelection(startInstance);
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // TODO Auto-generated method stub

    }

    public void elementChanged(ElementChangeEvent ece) {
        // TODO Auto-generated method stub

    }

    public void dispose() {
    }
}
