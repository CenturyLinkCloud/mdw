/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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
