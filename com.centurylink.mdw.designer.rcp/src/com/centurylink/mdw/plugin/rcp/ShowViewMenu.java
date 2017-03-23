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
package com.centurylink.mdw.plugin.rcp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IPluginContribution;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.activities.WorkbenchActivityHelper;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;

public class ShowViewMenu extends ContributionItem {
    private IWorkbenchWindow window;

    private Comparator<CommandContributionItemParameter> actionComparator = new Comparator<CommandContributionItemParameter>() {
        public int compare(CommandContributionItemParameter a1,
                CommandContributionItemParameter a2) {
            return a1.label.compareTo(a2.label);
        }
    };

    private boolean dirty = true;

    public boolean isDirty() {
        return dirty;
    }

    public boolean isDynamic() {
        return true;
    }

    private IMenuListener menuListener = new IMenuListener() {
        public void menuAboutToShow(IMenuManager manager) {
            manager.markDirty();
            dirty = true;
        }
    };

    public ShowViewMenu(IWorkbenchWindow window) {
        super("showViewMenu");
        this.window = window;
    }

    public void fill(Menu menu, int index) {
        if (getParent() instanceof MenuManager)
            ((MenuManager) getParent()).addMenuListener(menuListener);

        if (!dirty)
            return;

        MenuManager manager = new MenuManager();
        fillMenu(manager);
        IContributionItem items[] = manager.getItems();
        if (items.length == 0) {
            MenuItem item = new MenuItem(menu, SWT.NONE, index++);
            item.setText("<No Views>");
            item.setEnabled(false);
        }
        else {
            for (int i = 0; i < items.length; i++)
                items[i].fill(menu, index++);
        }
        dirty = false;
    }

    private void fillMenu(IMenuManager innerMgr) {
        innerMgr.removeAll();

        IWorkbenchPage page = window.getActivePage();
        if (page == null || page.getPerspective() == null)
            return;

        // visible actions
        List<String> viewIds = Arrays.asList(page.getShowViewShortcuts());

        List<CommandContributionItemParameter> actions = new ArrayList<CommandContributionItemParameter>(
                viewIds.size());
        for (String id : viewIds) {
            if (id.equals("org.eclipse.ui.internal.introview"))
                continue;
            CommandContributionItemParameter item = getItem(id);
            if (item != null)
                actions.add(item);
        }
        Collections.sort(actions, actionComparator);
        for (CommandContributionItemParameter ccip : actions) {
            if (WorkbenchActivityHelper.filterItem(ccip))
                continue;
            CommandContributionItem item = new CommandContributionItem(ccip);
            innerMgr.add(item);
        }
    }

    @SuppressWarnings("restriction")
    static class PluginCCIP extends CommandContributionItemParameter
            implements IPluginContribution {
        private String localId;

        public String getLocalId() {
            return localId;
        }

        private String pluginId;

        public String getPluginId() {
            return pluginId;
        }

        public PluginCCIP(IViewDescriptor v, IServiceLocator serviceLocator, String id,
                String commandId, int style) {
            super(serviceLocator, id, commandId, style);
            localId = ((org.eclipse.ui.internal.registry.ViewDescriptor) v).getLocalId();
            pluginId = ((org.eclipse.ui.internal.registry.ViewDescriptor) v).getPluginId();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private CommandContributionItemParameter getItem(String viewId) {
        IViewRegistry viewReg = Activator.getDefault().getWorkbench().getViewRegistry();
        IViewDescriptor viewDesc = viewReg.find(viewId);
        if (viewDesc == null)
            return null;
        String label = viewDesc.getLabel();

        CommandContributionItemParameter parms = new PluginCCIP(viewDesc, window, viewId,
                IWorkbenchCommandConstants.VIEWS_SHOW_VIEW, CommandContributionItem.STYLE_PUSH);
        parms.label = label;
        parms.icon = viewDesc.getImageDescriptor();
        parms.parameters = new HashMap();
        parms.parameters.put("org.eclipse.ui.views.showView.viewId", viewId);
        return parms;
    }

}
