/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.centurylink.mdw.taskmgr.ui.Lister;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.layout.ViewUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * UI List class for user admin.  Also maintains a static
 * list of user reference data loaded from the workflow.
 */
public class Users extends SortableList implements Lister
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public Users(ListUI listUI)
  {
    super(listUI);
  }

  /**
   *  Needs a no-arg constructor since Users is a dropdown lister
   *  in addition to being a SortableList implementation.
   */
  public Users() throws UIException
  {
    super(ViewUI.getInstance().getListUI("usersList"));
  }

  public static List<UserVO> getUsers()
  {
    return UserGroupCache.getUsers();
  }

  /**
   * Returns all the users associated with a workgroup.
   * @param groupName the name of the workgroup
   * @return array of model UserVO objects
   */
  public static UserVO[] getUsersInWorkgroup(String groupName) throws CachingException
  {
    if (UserGroupVO.COMMON_GROUP.equals(groupName))
      return UserGroupCache.getUsers().toArray(new UserVO[0]);
    else
      return UserGroupCache.getWorkgroup(groupName).getUsers();
  }

  /**
   * Returns all the users NOT currently associated with a workgroup.
   * @param groupName the name of the workgroup
   * @return array of model UserVO objects
   */
  public static UserVO[] getUsersNotInWorkgroup(String groupName) throws CachingException
  {

    if (UserGroupVO.COMMON_GROUP.equals(groupName))
      return new UserVO[0];

    List<UserVO> usersInWorkgroup = Arrays.asList(UserGroupCache.getWorkgroup(groupName).getUsers());

    List<UserVO> usersNotInWorkgroup = new ArrayList<UserVO>();

    for (UserVO user : getUsers())
    {
      if (!usersInWorkgroup.contains(user))
        usersNotInWorkgroup.add(user);
    }

    Collections.sort(usersNotInWorkgroup);
    return (UserVO[]) usersNotInWorkgroup.toArray(new UserVO[0]);
  }

  public static UserVO getUserByCuid(String cuid) throws CachingException
  {
    return UserGroupCache.getUser(cuid);
  }

  public static UserVO getUserByName(String name)
  {
    for (UserVO user : getUsers())
    {
      if (name.equals(user.getName()))
        return user;
    }
    return null;
  }

  /**
   * Returns the users associated with the current user's workgroups.
   *
   * @return array of UserVOs
   */
  public static UserVO[] getUsersForUserWorkgroups() throws CachingException
  {
    List<UserVO> uniqueUsers = new ArrayList<UserVO>();

    String[] userWorkgroups = FacesVariableUtil.getCurrentUser().getWorkgroupNames();
    for (String userWorkgroup : userWorkgroups)
    {
      if (!UserGroupVO.COMMON_GROUP.equals(userWorkgroup))
      {
        UserGroupVO workgroup = UserGroupCache.getWorkgroup(userWorkgroup);
        for (UserVO user : workgroup.getUsers())
        {
          if (!uniqueUsers.contains(user))
            uniqueUsers.add(user);
        }
      }
    }

    Collections.sort(uniqueUsers);
    return (UserVO[])uniqueUsers.toArray(new UserVO[0]);
  }

  /**
   * Returns the users associated with passed work groups
   *
   * @return array of UserVOs
   */
  public static UserVO[] getUsersForWorkgroups(String[] groupNames) throws CachingException
  {
    List<UserVO> uniqueUsers = new ArrayList<UserVO>();
    for (String groupName : groupNames)
    {
      if (!UserGroupVO.COMMON_GROUP.equals(groupName))
      {
        UserGroupVO workgroup = UserGroupCache.getWorkgroup(groupName);
        for (UserVO user : workgroup.getUsers())
        {
          if (!uniqueUsers.contains(user))
            uniqueUsers.add(user);
        }
      }
    }

    Collections.sort(uniqueUsers);
    return (UserVO[])uniqueUsers.toArray(new UserVO[0]);
  }

  public static void remove(UserVO user)
  {
    UserGroupCache.remove(user);
    syncList("usersList");
  }

  /**
   * Get a list of SelectItems populated from an array of user VOs.
   *
   * @param users the user VOs to include in the list
   * @param firstItem the message for an optional first item (null for none)
   * @return list of SelectItems
   */
  public static List<SelectItem> getUserSelectItems(UserVO[] users, String firstItem)
  {
    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    if (firstItem != null)
      selectItems.add(new SelectItem("0", firstItem));
    for (int i = 0; i < users.length; i++)
    {
      UserVO user = users[i];
      selectItems.add(new SelectItem(user.getId().toString(), user.getName() == null ? user.getCuid() : user.getName()));
    }

    return selectItems;
  }

  /**
   * Get a list of SelectItems populated from an array of User Roles.
   *
   * @param roles - the UserRole[] to include
   * @return list of SelectItems
   */
  public static List<SelectItem> getUserRoleSelectItems(List<String> roles)
  {
    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    for (int i = 0; i < roles.size(); i++)
    {
      SelectItem selectItem = new SelectItem(roles.get(i), roles.get(i));
      selectItems.add(selectItem);
    }

    return selectItems;
  }

  /**
   * Get a list of SelectItems populated from an array of user groups.
   *
   * @param groups - the UserGroupVO[] to include
   * @return list of SelectItems
   */
  public static List<SelectItem> getUserGroupSelectItems(UserGroupVO[] groups)
  {
    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    for (int i = 0; i < groups.length; i++)
    {
      UserGroupVO group = groups[i];
      if (!group.getName().equals(UserGroupVO.COMMON_GROUP))
        selectItems.add(new SelectItem(group.getName(), group.getName()));
    }

    return selectItems;
  }


  /**
   * Get a list of SelectItems based on the task id.
   *
   * @param task id
   * @param firstItem the message for an optional first item (null for none)
   * @return list of SelectItems for the task
   */
  public List<SelectItem> getUserSelectItemsForTask(Long taskId, String firstItem)
  {
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      UserManager userMgr = RemoteLocator.getUserManager();
      List<String> groups = taskMgr.getGroupsForTask(taskId);
      UserVO[] users = userMgr.getUsersForGroups(groups.toArray(new String[groups.size()]));

      List<SelectItem> selectItems = new ArrayList<SelectItem>();

      if (firstItem != null)
      {
        selectItems.add(new SelectItem("0", firstItem));
      }
      for (int i = 0; i < users.length; i++)
      {
        UserVO userForTask = users[i];
        selectItems.add(new SelectItem(userForTask.getId().toString(), userForTask.getCuid()));
      }

      return selectItems;
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * Find a user in the list based on its ID.
   * @param userId
   * @return the user VO, or null if not found
   */
  public static UserVO getUser(Long userId) throws CachingException
  {
    return UserGroupCache.getUser(userId);
  }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.Decoder#decode(java.lang.Long)
   */
  public String decode(Long id)
  {
    for (UserVO user : getUsers())
    {
      if (user.getId().equals(id))
        return user.getCuid();
    }
    return null;
  }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.Lister#list(java.lang.String)
   */
  public List<SelectItem> list()
  {
    List<SelectItem> list = new ArrayList<SelectItem>();
    try
    {
      list.addAll(getUserSelectItems(getUsersForUserWorkgroups(), ""));
    }
    catch (CachingException ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
    return list;
  }

  public List<SelectItem> list(String firstItem)
  {
    try
    {
      return getUserSelectItems(getUsersForUserWorkgroups(), firstItem);
    }
    catch (CachingException ex)
    {
      logger.severeException(ex.getMessage(),  ex);
      return getUserSelectItems(new UserVO[0], null);
    }
  }

  public List<SelectItem> getList()
  {
    return list();
  }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.list.SortableList#retrieveItems()
   */
  protected DataModel<ListItem> retrieveItems()
  {
    try
    {
      return new ListDataModel<ListItem>(convertUserVOs(getUsers()));
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * Converts a collection of users
   */
  protected List<ListItem> convertUserVOs(List<UserVO> users)
  {
    List<ListItem> rowList = new ArrayList<ListItem>();

    for (UserVO user : users)
    {
      UserItem item = new UserItem(user);
      rowList.add(item);
    }
    return rowList;
  }

  @Override
  public boolean isSortable()
  {
    return !UserGroupCache.getUsers().isEmpty();
  }
}
