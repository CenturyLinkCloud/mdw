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

import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;

/**
 * Listener for process editor context events.
 */
public class ProcessEditorContextListener extends WorkflowEditorPartListener {
    public ProcessEditorContextListener(WorkflowProcess processVersion) {
        super(processVersion);
    }

    public void broughtToTop(WorkflowElementEditor editor) {
        ProcessEditor processEditor = (ProcessEditor) editor;
        // force canvas to assume current link shape
        processEditor.setCanvasLinkStyle(ProcessEditorActionBarContributor.getLinkStyle());
        processEditor.updateCanvasBackground();
    }

    public void closed(WorkflowElementEditor editor) {
        ProcessEditor processEditor = (ProcessEditor) editor;
        processEditor.remove();
    }
}
