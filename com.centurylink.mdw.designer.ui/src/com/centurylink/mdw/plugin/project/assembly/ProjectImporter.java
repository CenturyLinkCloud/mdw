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
package com.centurylink.mdw.plugin.project.assembly;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ProjectImporter {
    private List<WorkflowProject> projectsToImport;
    private Shell shell;

    public ProjectImporter(List<WorkflowProject> projectsToImport) {
        this.projectsToImport = projectsToImport;
    }

    public ProjectImporter(WorkflowProject projectToImport) {
        this.projectsToImport = new ArrayList<WorkflowProject>();
        this.projectsToImport.add(projectToImport);
    }

    public void doImport() {
        shell = MdwPlugin.getActiveWorkbenchWindow().getShell();

        BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
            public void run() {
                for (WorkflowProject toImport : projectsToImport)
                    WorkflowProjectManager.addProject(toImport);
            }
        });
    }
}
