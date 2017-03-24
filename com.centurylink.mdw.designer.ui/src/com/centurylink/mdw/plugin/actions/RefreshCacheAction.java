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
package com.centurylink.mdw.plugin.actions;

import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchWindow;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class RefreshCacheAction extends BasePulldownAction {
    private Image image;
    private WorkflowProject mostRecentCacheRefreshWorkflowProject;

    public void init(IWorkbenchWindow window) {
        super.init(window);
        image = MdwPlugin.getImageDescriptor("icons/refr_cache.gif").createImage();
    }

    public void run(IAction action) {
        if (mostRecentCacheRefreshWorkflowProject != null)
            mostRecentCacheRefreshWorkflowProject.getDesignerProxy().getCacheRefresh()
                    .doRefresh(false);
    }

    /**
     * populates the plugin action menu (the refr_cache icon) with its items
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
                String projName = workflowProject.isFrameworkProject() ? "MDWFramework"
                        : workflowProject.getSourceProjectName();

                MenuItem item = new MenuItem(menu, SWT.NONE);
                item.setText(projName + " Refresh");
                item.setImage(image);
                item.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        if (!workflowProject.isUserAuthorizedForSystemAdmin()) {
                            PluginMessages.uiMessage(
                                    "You must be in the System Admin role to refresh the cache for this environment",
                                    "Not Authorized", PluginMessages.INFO_MESSAGE);
                        }
                        else {
                            mostRecentCacheRefreshWorkflowProject = workflowProject;
                            workflowProject.getDesignerProxy().getCacheRefresh().doRefresh(false);
                        }
                    }
                });
            }
        }
    }
}