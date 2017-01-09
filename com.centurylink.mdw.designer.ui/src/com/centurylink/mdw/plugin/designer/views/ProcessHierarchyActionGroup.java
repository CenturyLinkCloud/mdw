/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import org.eclipse.ui.actions.ActionGroup;

import com.centurylink.mdw.plugin.actions.WorkflowElementActionHandler;

public class ProcessHierarchyActionGroup extends ActionGroup
{
  ProcessHierarchyView view;
  private WorkflowElementActionHandler actionHandler;
  WorkflowElementActionHandler getActionHandler() { return actionHandler; }

  public ProcessHierarchyActionGroup(ProcessHierarchyView view)
  {
    this.view = view;
    this.actionHandler = new WorkflowElementActionHandler();
  }

}
