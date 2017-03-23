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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;

/**
 * Custom menu manager that prevents unwanted menu contributions from being
 * displayed in context menus. All menu items must begin with MDW_MENU_PREFIX in
 * order to be displayed.
 */
public class MdwMenuManager extends MenuManager {
    public static final String MDW_MENU_PREFIX = "com.centurylink.mdw.menu.";

    public MdwMenuManager(String title) {
        super(title);
    }

    @Override
    public IContributionItem[] getItems() {
        IContributionItem[] items = super.getItems();
        List<IContributionItem> mdwItems = null;
        if (items != null) {
            mdwItems = new ArrayList<IContributionItem>();
            for (IContributionItem item : items) {
                if (item != null && item.getId() != null
                        && item.getId().startsWith(MDW_MENU_PREFIX))
                    mdwItems.add(item);
            }
        }
        return mdwItems == null ? null : mdwItems.toArray(new IContributionItem[0]);
    }
}
