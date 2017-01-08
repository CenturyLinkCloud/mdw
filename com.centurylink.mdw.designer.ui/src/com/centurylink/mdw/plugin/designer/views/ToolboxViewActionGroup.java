/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionGroup;

import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

public class ToolboxViewActionGroup extends ActionGroup {
  public static final String TOOLBOX_SUPPRESSED_IMPLS = "MdwToolboxSuppressedActivityImplementors";

  private ToolboxView view;

  private IAction sortAction;
  private IAction refreshAction;
  private IAction saveAction;
  public IAction getSaveAction() { return saveAction; }
  private IAction deleteAction;
  public IAction getDeleteAction() { return deleteAction; }
  private IAction newAction;
  public IAction getNewAction() { return newAction; }
  private IAction filterAction;
  public IAction getFilterAction() { return filterAction; }
  private IAction discoverAction;
  public IAction getDiscoverAction() { return discoverAction; }

  public ToolboxViewActionGroup(ToolboxView view)
  {
    this.view = view;

    sortAction = createSortAction();
    refreshAction = createRefreshAction();
    saveAction = createSaveAction();
    deleteAction = createDeleteAction();
    newAction = createNewAction();
    filterAction = createFilterAction();
    discoverAction = createDiscoverAction();
    enableToolbarActions(false);
  }

  public void enableToolbarActions(boolean enabled)
  {
    sortAction.setEnabled(enabled);
    refreshAction.setEnabled(enabled);
    ActivityImpl impl = view.getSelection();
    saveAction.setEnabled(enabled && view.isDirty() && impl != null && impl.isUserAuthorized(UserRoleVO.ASSET_DESIGN));
    deleteAction.setEnabled(enabled && view.isSelection() && impl != null && impl.isUserAuthorized(UserRoleVO.ASSET_DESIGN));
    newAction.setEnabled(enabled && impl != null && impl.isUserAuthorized(UserRoleVO.ASSET_DESIGN));
    filterAction.setEnabled(enabled);
    discoverAction.setEnabled(enabled && impl != null && impl.isUserAuthorized(UserRoleVO.ASSET_DESIGN));
  }

  private IAction createSortAction()
  {
    IAction sortAction = new Action()
    {
      public void run()
      {
        view.handleSort(isChecked());
      }
    };
    sortAction.setText("Sort");
    ImageDescriptor sortImageDesc = MdwPlugin.getImageDescriptor("icons/sort.gif");
    sortAction.setImageDescriptor(sortImageDesc);
    sortAction.setChecked(MdwPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.PREFS_SORT_TOOLBOX_A_TO_Z));
    return sortAction;
  }

  private IAction createRefreshAction()
  {
    IAction refreshAction = new Action()
    {
      public void run()
      {
        view.handleRefresh();
      }
    };
    refreshAction.setText("Refresh");
    ImageDescriptor refreshImageDesc = MdwPlugin.getImageDescriptor("icons/refresh.gif");
    refreshAction.setImageDescriptor(refreshImageDesc);
    return refreshAction;
  }

  private IAction createSaveAction()
  {
    IAction saveAction = new Action()
    {
      public void run()
      {
        view.handleSave();
      }
    };
    saveAction.setText("Save");
    ImageDescriptor saveImageDesc = MdwPlugin.getImageDescriptor("icons/save.gif");
    saveAction.setImageDescriptor(saveImageDesc);
    ImageDescriptor saveDisabledImageDesc = MdwPlugin.getImageDescriptor("icons/save_disabled.gif");
    saveAction.setDisabledImageDescriptor(saveDisabledImageDesc);
    return saveAction;
  }

  private IAction createDeleteAction()
  {
    IAction deleteAction = new Action()
    {
      public void run()
      {
        view.handleDelete();
      }
    };
    deleteAction.setText("Delete");
    ImageDescriptor deleteImageDesc = MdwPlugin.getImageDescriptor("icons/delete.gif");
    deleteAction.setImageDescriptor(deleteImageDesc);
    return deleteAction;
  }

  private IAction createNewAction()
  {
    IAction newAction = new Action()
    {
      public void run()
      {
        view.handleNew();
      }
    };
    newAction.setText("New Implementor");
    ImageDescriptor newImageDesc = MdwPlugin.getImageDescriptor("icons/genact_wiz.gif");
    newAction.setImageDescriptor(newImageDesc);
    return newAction;
  }

  private IAction createFilterAction()
  {
    IAction filterAction = new Action()
    {
      public void run()
      {
        view.handleFilter();
      }
    };

    filterAction.setText("Filter");
    ImageDescriptor filterImageDesc = MdwPlugin.getImageDescriptor("icons/filter.gif");
    filterAction.setImageDescriptor(filterImageDesc);
    return filterAction;
  }

  private IAction createDiscoverAction()
  {
    IAction discoverAction = new Action()
    {
      public void run()
      {
        view.handleDiscover();
      }
    };

    discoverAction.setText("Discover Workflow Assets");
    ImageDescriptor discoverImageDesc = MdwPlugin.getImageDescriptor("icons/discover.gif");
    discoverAction.setImageDescriptor(discoverImageDesc);
    return discoverAction;
  }


  @Override
  public void fillActionBars(IActionBars actionBars)
  {
    super.fillActionBars(actionBars);
    IToolBarManager toolbar = actionBars.getToolBarManager();
    toolbar.add(new GroupMarker("mdw.toolbox.group"));
    toolbar.add(sortAction);
    toolbar.add(filterAction);
    toolbar.add(refreshAction);
    toolbar.add(saveAction);
    toolbar.add(discoverAction);
    toolbar.add(newAction);
    toolbar.add(deleteAction);
  }
}