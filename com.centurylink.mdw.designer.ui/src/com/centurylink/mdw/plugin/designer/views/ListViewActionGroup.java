/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import org.eclipse.ui.actions.ActionGroup;

// TODO provide actions based on listview config (TaskView.xml)
public class ListViewActionGroup extends ActionGroup
{
  private ListView listView;
  public ListView getListView() { return listView; }
  
  public ListViewActionGroup(ListView listView)
  {
    this.listView = listView;
  }
}
