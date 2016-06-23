/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.user;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.faces.event.ValueChangeEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.centurylink.mdw.taskmgr.ui.EditableItemActionController;
import com.centurylink.mdw.taskmgr.ui.workgroups.WorkgroupTree;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

public class UserActionController extends EditableItemActionController
{
  public static final String CONTROLLER_BEAN = "userActionController";

  public static final String ACTION_ADD_USER = "AddUser";
  public static final String ACTION_UPDATE_USER = "UpdateUser";
  public static final String ACTION_DELETE_USER = "DeleteUser";

  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private String[] _selectedIds;

  /**
   * Called from command links in the users list.
   *
   * @return the nav destination.
   */
  public String performAction(String action, ListItem listItem) throws UIException
  {
    super.performAction(action, listItem);
    FacesVariableUtil.setValue(CONTROLLER_BEAN, this);
    UserItem userItem = (UserItem) listItem;
    if (ACTION_EDIT.equals(action))
      WorkgroupTree.getInstance().setUser(userItem.getUser());
    else if (ACTION_DELETE.equals(action))
      WorkgroupTree.getInstance().setDeletePending(userItem.getUser());

    return null;
  }

  /**
   * Called as a ValueChangeListener when the ids selected in the roles or workgroup
   * picklists have been updated.  Selected ids can be for either roleIds or
   * workgroupIds.  The variable _saveAction keeps track of what's being saved.
   *
   * @param event the ValueChangeEvent
   */
  public void selectedValueChanged(ValueChangeEvent event)
  {
    List<String> ids = new ArrayList<String>();
    StringTokenizer st = new StringTokenizer(event.getNewValue().toString(), ",");
    while (st.hasMoreTokens())
    {
      ids.add(new String(st.nextToken()));
    }
    _selectedIds = (String[]) ids.toArray(new String[0]);
  }

  /**
   * Ajax action methods for MDW 5.2.
   */
  public String saveUserWithGroups() throws UIException
  {
    UserItem userItem = (UserItem) FacesVariableUtil.getValue(UserItem.ITEM_BEAN);
    setItem(userItem);
    boolean add = userItem.getId() == null;
    if (TaskManagerAccess.getInstance().isRemoteDetail())
    {
      notifyDetailTaskManagers(add ? ACTION_ADD_USER : ACTION_UPDATE_USER, userItem.getUser());
    }
    userItem.save();
    if (_selectedIds != null)
      userItem.saveUserWorkgroups(_selectedIds);
    return null;
  }

  public String saveUserWithGroupRoles() throws UIException
  {
    UserItem userItem = (UserItem)FacesVariableUtil.getValue(UserItem.ITEM_BEAN);
    setItem(userItem);

    if (TaskManagerAccess.getInstance().isRemoteDetail())
    {
        String parentGroup = userItem.getParentGroup() == null ? UserGroupVO.COMMON_GROUP : userItem.getParentGroup().getName();
        final UserGroupVO groupWithRoles = userItem.getUser().getUserGroup(parentGroup);
        try
        {
          notifyDetailTaskManagers(ACTION_UPDATE_USER, new UserVO(userItem.getUser().getJson())
          {
            public JSONObject getJson() throws JSONException
            {
              // inject JSON for the scopedUser group roles
              JSONObject json = super.getJson();
              if (groupWithRoles != null)
              {
                JSONArray groups = json.getJSONArray("groups");
                for (int i = 0; i < groups.length(); i++)
                {
                  String group = groups.getString(i);
                  if (group.equals(groupWithRoles.getName()))
                  {
                    JSONArray groupRoles = new JSONArray();
                    if (groupWithRoles.getRoles() != null)
                    {
                      for (String role : groupWithRoles.getRoles())
                        groupRoles.put(role);
                    }
                    json.put(groupWithRoles.getName(), groupRoles);
                  }
                }
              }
              return json;
            }
          });
        }
        catch (JSONException ex)
        {
          throw new UIException(ex.getMessage(), ex);
        }
    }
    userItem.save();
    if (_selectedIds != null)
      userItem.saveUserGroupRoles(_selectedIds);
    return null;
  }

  public String deleteUser() throws UIException
  {
    UserItem userItem = (UserItem) FacesVariableUtil.getValue(UserItem.ITEM_BEAN);
    setItem(userItem);

    if (TaskManagerAccess.getInstance().isRemoteDetail())
    {
      notifyDetailTaskManagers(ACTION_DELETE_USER, userItem.getUser());
    }
    userItem.delete();
    return null;
  }

  public String saveUserWithPreferences()
  {
    UserItem userItem = (UserItem) FacesVariableUtil.getValue(UserItem.ITEM_BEAN);
    setItem(userItem);
    try
    {
      if (TaskManagerAccess.getInstance().isRemoteDetail())
      {
        notifyDetailTaskManagers(ACTION_UPDATE_USER, userItem.getUser());
      }
      userItem.save();
      userItem.savePreferences();
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage(ex);
      return "go_user";
    }

    return "go_myTasks";
  }

  public String goMyUserInfo() throws UIException
  {
    if (TaskManagerAccess.getInstance().isRemoteSummary())
    {
      try
      {
        FacesVariableUtil.navigate(new URL(TaskManagerAccess.getInstance().getSummaryTaskManagerUrl() + "/admin/userInfo.jsf"));
        return null;
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
        throw new UIException(ex.getMessage(), ex);
      }
    }
    else
    {
      return "go_user";
    }
  }
}
