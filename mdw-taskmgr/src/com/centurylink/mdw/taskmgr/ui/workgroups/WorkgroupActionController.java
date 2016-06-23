/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.workgroups;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.faces.event.ValueChangeEvent;

import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.centurylink.mdw.taskmgr.ui.EditableItemActionController;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

public class WorkgroupActionController extends EditableItemActionController
{
  public static final String CONTROLLER_BEAN = "workgroupActionController";

  public static final String ACTION_ADD_GROUP = "AddGroup";
  public static final String ACTION_UPDATE_GROUP = "UpdateGroup";
  public static final String ACTION_DELETE_GROUP = "DeleteGroup";

  private Long[] _selectedIds;

  /**
   * Called from command links in the group list.
   *
   * @return the nav destination.
   */
  public String performAction(String action, ListItem listItem) throws UIException
  {
    super.performAction(action, listItem);
    FacesVariableUtil.setValue(CONTROLLER_BEAN, this);
    WorkgroupItem groupItem = (WorkgroupItem) listItem;
    if (ACTION_EDIT.equals(action))
      WorkgroupTree.getInstance().setGroup(groupItem.getUserGroup());
    else if (ACTION_DELETE.equals(action))
      WorkgroupTree.getInstance().setDeletePending(groupItem.getUserGroup());

    return null;
  }

  /**
   * Called as a ValueChangeListener when the ids selected in the workgroup
   * picklist have been updated.  Selected ids can be for either taskIds or
   * userIds.  The variable mSaveAction keeps track of what's being saved.
   *
   * @param event the ValueChangeEvent
   */
  public void selectedValueChanged(ValueChangeEvent event)
  {
    List<Long> ids = new ArrayList<Long>();
    StringTokenizer st = new StringTokenizer(event.getNewValue().toString(), ",");
    while (st.hasMoreTokens())
    {
      ids.add(new Long(st.nextToken()));
    }
    _selectedIds = (Long[]) ids.toArray(new Long[0]);
  }

  /**
   * Ajax action methods for MDW 5.2.
   */
  public String saveGroupWithUsers() throws UIException
  {
    WorkgroupItem groupItem = (WorkgroupItem)FacesVariableUtil.getValue(WorkgroupItem.ITEM_BEAN);
    setItem(groupItem);
    boolean add = groupItem.getId() == null;
    if (TaskManagerAccess.getInstance().isRemoteDetail())
      notifyDetailTaskManagers(add ? ACTION_ADD_GROUP : ACTION_UPDATE_GROUP, groupItem.getUserGroup());
    groupItem.save();
    if (_selectedIds != null)
      groupItem.saveWorkgroupUsers(_selectedIds);
    return null;
  }

  public String deleteGroup() throws UIException
  {
    WorkgroupItem groupItem = (WorkgroupItem)FacesVariableUtil.getValue(WorkgroupItem.ITEM_BEAN);
    setItem(groupItem);
    if (TaskManagerAccess.getInstance().isRemoteDetail())
      notifyDetailTaskManagers(ACTION_DELETE_GROUP, groupItem.getUserGroup());
    groupItem.delete();
    return null;
  }
}
