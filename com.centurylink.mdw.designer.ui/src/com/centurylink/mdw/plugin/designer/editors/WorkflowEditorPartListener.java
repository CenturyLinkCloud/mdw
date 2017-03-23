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

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;

import com.centurylink.mdw.plugin.designer.model.WorkflowElement;

/**
 * Base class for responding to editor lifecycle events.
 */
public abstract class WorkflowEditorPartListener implements IPartListener2 {
    private WorkflowElement workflowElement;

    public WorkflowEditorPartListener(WorkflowElement workflowElement) {
        this.workflowElement = workflowElement;
    }

    // override these methods to respond to editor events
    public void activated(WorkflowElementEditor editor) {
    };

    public void broughtToTop(WorkflowElementEditor editor) {
    };

    public void closed(WorkflowElementEditor editor) {
    };

    public void hidden(WorkflowElementEditor editor) {
    };

    public void inputChanged(WorkflowElementEditor editor) {
    };

    public void visible(WorkflowElementEditor editor) {
    };

    public void opened(WorkflowElementEditor editor) {
    };

    public void deactivated(WorkflowElementEditor editor) {
    };

    public void partActivated(IWorkbenchPartReference partRef) {
        WorkflowElementEditor editor = getAffectedEditor(partRef);
        if (editor != null) {
            activated(editor);
        }
    }

    public void partBroughtToTop(IWorkbenchPartReference partRef) {
        WorkflowElementEditor editor = getAffectedEditor(partRef);
        if (editor != null) {
            broughtToTop(editor);
        }
    }

    public void partClosed(IWorkbenchPartReference partRef) {
        WorkflowElementEditor editor = getAffectedEditor(partRef);
        if (editor != null) {
            closed(editor);
        }
    }

    public void partDeactivated(IWorkbenchPartReference partRef) {
        WorkflowElementEditor editor = getAffectedEditor(partRef);
        if (editor != null) {
            deactivated(editor);
        }
    }

    public void partHidden(IWorkbenchPartReference partRef) {
        WorkflowElementEditor editor = getAffectedEditor(partRef);
        if (editor != null) {
            hidden(editor);
        }
    }

    public void partInputChanged(IWorkbenchPartReference partRef) {
        WorkflowElementEditor editor = getAffectedEditor(partRef);
        if (editor != null) {
            inputChanged(editor);
        }
    }

    public void partOpened(IWorkbenchPartReference partRef) {
        WorkflowElementEditor editor = getAffectedEditor(partRef);
        if (editor != null) {
            opened(editor);
        }
    }

    public void partVisible(IWorkbenchPartReference partRef) {
        WorkflowElementEditor editor = getAffectedEditor(partRef);
        if (editor != null) {
            visible(editor);
        }
    }

    private WorkflowElementEditor getAffectedEditor(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef.getPart(false);
        if (part instanceof WorkflowElementEditor) {
            WorkflowElementEditor editor = (WorkflowElementEditor) part;
            if (editor.getElement().equals(workflowElement)) {
                return editor;
            }
        }
        return null;
    }
}
