/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.actions;

import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.actions.WebLaunchActions.WebApp;
import com.centurylink.mdw.plugin.actions.WebLaunchActions.WebLaunchAction;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class MdwHubAction extends BasePulldownAction implements IObjectActionDelegate {
    private WorkflowProject mostRecentHubWorkflowProject;

    public void init(IWorkbenchWindow window) {
        super.init(window);
    }

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    }

    public void run(IAction action) {
        WorkflowProject workflowProject = getProject(getSelection());
        WebLaunchAction launchAction = WebLaunchActions.getLaunchAction(workflowProject,
                WebApp.MdwHub);
        if (workflowProject != null)
            launchAction.launch(workflowProject);
        else if (mostRecentHubWorkflowProject != null)
            launchAction.launch(mostRecentHubWorkflowProject);
    }

    /**
     * populates the plugin action menu (the mdwhub icon) with its items
     */
    public void populateMenu(Menu menu) {
        WorkflowProjectManager wfProjectMgr = WorkflowProjectManager.getInstance();
        List<WorkflowProject> workflowProjects = wfProjectMgr.getWorkflowProjects();
        if (workflowProjects.isEmpty()) {
            MenuItem item = new MenuItem(menu, SWT.NONE);
            item.setText("(No Projects)");
            item.setImage(MdwPlugin.getImageDescriptor("icons/wait.gif").createImage());
            item.setEnabled(false);
        }
        else {
            for (final WorkflowProject workflowProject : workflowProjects) {
                if (workflowProject.checkRequiredVersion(5, 5)) {
                    String projName = workflowProject.isFrameworkProject() ? "MDWFramework"
                            : workflowProject.getName();

                    // MDWWeb
                    MenuItem item = new MenuItem(menu, SWT.NONE);
                    final WebLaunchAction launchAction = WebLaunchActions
                            .getLaunchAction(workflowProject, WebApp.MdwHub);
                    item.setText(projName + " - " + launchAction.getLabel());
                    item.setImage(launchAction.getIconImage());
                    item.addSelectionListener(new SelectionAdapter() {
                        public void widgetSelected(SelectionEvent e) {
                            mostRecentHubWorkflowProject = workflowProject;
                            launchAction.launch(workflowProject);
                        }
                    });
                }
            }
        }
    }
}