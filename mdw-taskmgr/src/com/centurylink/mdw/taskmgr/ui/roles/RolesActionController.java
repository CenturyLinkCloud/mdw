/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.roles;

import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.centurylink.mdw.taskmgr.ui.EditableItemActionController;
import com.centurylink.mdw.taskmgr.ui.workgroups.WorkgroupTree;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

public class RolesActionController extends EditableItemActionController
{
  public static final String CONTROLLER_BEAN = "rolesActionController";

  public static final String ACTION_ADD_ROLE = "AddRole";
  public static final String ACTION_UPDATE_ROLE = "UpdateRole";
  public static final String ACTION_DELETE_ROLE = "DeleteRole";

  /**
   * Called from command links in the roles list.
   *
   * @return the nav destination.
   */
  public String performAction(String action, ListItem listItem) throws UIException
  {
    super.performAction(action, listItem);
    FacesVariableUtil.setValue(CONTROLLER_BEAN, this);
    RoleItem roleItem = (RoleItem) listItem;
    if (ACTION_EDIT.equals(action))
      WorkgroupTree.getInstance().setRole(roleItem.getRole());
    else if (ACTION_DELETE.equals(action))
      WorkgroupTree.getInstance().setDeletePending(roleItem.getRole());

    return null;
  }

  /**
   * Ajax action methods for MDW 5.2.
   */
  public String saveRole() throws UIException
  {
    RoleItem roleItem = (RoleItem)FacesVariableUtil.getValue(RoleItem.ITEM_BEAN);
    setItem(roleItem);
    boolean add = roleItem.getId() == null;
    if (TaskManagerAccess.getInstance().isRemoteDetail())
      notifyDetailTaskManagers(add ? ACTION_ADD_ROLE : ACTION_UPDATE_ROLE, roleItem.getRole());
    roleItem.save();
    return null;
  }

  public String deleteRole() throws UIException
  {
    RoleItem roleItem = (RoleItem)FacesVariableUtil.getValue(RoleItem.ITEM_BEAN);
    setItem(roleItem);
    if (TaskManagerAccess.getInstance().isRemoteDetail())
      notifyDetailTaskManagers(ACTION_DELETE_ROLE, roleItem.getRole());
    roleItem.delete();
    return null;
  }
}

