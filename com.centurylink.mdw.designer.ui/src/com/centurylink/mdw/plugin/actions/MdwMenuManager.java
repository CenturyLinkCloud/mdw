/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;

/**
 * Custom menu manager that prevents unwanted menu contributions from being displayed
 * in context menus.  All menu items must begin with MDW_MENU_PREFIX in order to be displayed.
 */
public class MdwMenuManager extends MenuManager
{
  public static final String MDW_MENU_PREFIX = "com.centurylink.mdw.menu.";

  public MdwMenuManager(String title)
  {
    super(title);
  }

  @Override
  public IContributionItem[] getItems()
  {
    IContributionItem[] items = super.getItems();
    List<IContributionItem> mdwItems = null;
    if (items != null)
    {
      mdwItems = new ArrayList<IContributionItem>();
      for (IContributionItem item : items)
      {
        if (item != null && item.getId() != null && item.getId().startsWith(MDW_MENU_PREFIX))
          mdwItems.add(item);
      }
    }
    return mdwItems == null ? null : mdwItems.toArray(new IContributionItem[0]);
  }
}
