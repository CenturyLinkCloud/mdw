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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate;

import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public abstract class BasePulldownAction implements IWorkbenchWindowPulldownDelegate {
    private IWorkbenchWindow activeWindow;

    public IWorkbenchWindow getActiveWindow() {
        return activeWindow;
    }

    private ISelection selection;

    public ISelection getSelection() {
        return selection;
    }

    public void init(IWorkbenchWindow window) {
        activeWindow = window;
    }

    public abstract void populateMenu(Menu menu);

    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

    public Menu getMenu(Control parent) {
        Menu menu = new Menu(parent);
        return createMenu(menu, false);
    }

    /**
     * creates the dropdown menu for the action
     */
    private Menu createMenu(Menu menu, final boolean wantFastAccess) {
        menu.addMenuListener(new MenuAdapter() {
            public void menuShown(MenuEvent e) {
                Menu menu = (Menu) e.widget;
                MenuItem[] items = menu.getItems();
                for (int i = 0; i < items.length; i++)
                    items[i].dispose();
                populateMenu(menu);
            }
        });

        return menu;
    }

    public void run(IAction action) {
    }

    public void dispose() {
    }

    protected WorkflowProject getProject(ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            Object first = ((IStructuredSelection) selection).getFirstElement();
            if (first instanceof IProject)
                return WorkflowProjectManager.getInstance().getWorkflowProject((IProject) first);
        }
        return null;
    }
}
