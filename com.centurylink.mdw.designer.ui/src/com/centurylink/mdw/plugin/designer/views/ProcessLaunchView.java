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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.internal.browser.WebBrowserView;

import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

@SuppressWarnings("restriction")
public class ProcessLaunchView extends WebBrowserView {
    private WorkflowProcess process;

    public WorkflowProcess getProcess() {
        return process;
    }

    public void setProcess(WorkflowProcess process) {
        this.process = process;
        WorkflowProject workflowProject = process.getProject();
        String procLaunchPath = "/facelets/process/plainLaunch.jsf";
        String processIdParam = "processId=" + process.getId();
        setURL(workflowProject.getTaskManagerUrl() + procLaunchPath + "?" + processIdParam);
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
    }
}
