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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionGroup;

import com.centurylink.mdw.plugin.MdwPlugin;

public class ProcessInstanceActionGroup extends ActionGroup {
    private ProcessInstanceListView view;

    private IAction refreshAction;
    private IAction filterAction;
    private IAction pageDownAction;
    private IAction pageUpAction;

    public ProcessInstanceActionGroup(ProcessInstanceListView view) {
        this.view = view;

        refreshAction = createRefreshAction();
        filterAction = createFilterAction();
        pageDownAction = createPageDownAction();
        pageUpAction = createPageUpAction();
    }

    private IAction createRefreshAction() {
        IAction action = new Action() {
            public void run() {
                view.refresh();
            }
        };
        action.setText("Refresh");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/refresh.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createFilterAction() {
        IAction action = new Action() {
            public void run() {
                view.filter();
            }
        };
        action.setText("Filter");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/filter.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createPageDownAction() {
        IAction action = new Action() {
            public void run() {
                view.pageDown();
            }
        };
        action.setText("Next Page");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/down.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    private IAction createPageUpAction() {
        IAction action = new Action() {
            public void run() {
                view.pageUp();
            }
        };
        action.setText("Previous Page");
        ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/up.gif");
        action.setImageDescriptor(imageDesc);
        return action;
    }

    @Override
    public void fillActionBars(IActionBars actionBars) {
        super.fillActionBars(actionBars);
        IToolBarManager toolbar = actionBars.getToolBarManager();
        toolbar.add(new GroupMarker("mdw.process.instance.group"));
        toolbar.add(refreshAction);
        toolbar.add(filterAction);
        toolbar.add(pageDownAction);
        toolbar.add(pageUpAction);
    }
}