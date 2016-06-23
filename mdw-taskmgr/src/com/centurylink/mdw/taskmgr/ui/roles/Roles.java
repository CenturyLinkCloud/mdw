/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.roles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.web.ui.list.ListItem;

public class Roles extends SortableList
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public Roles(ListUI listUI)
  {
    super(listUI);
  }

  public static List<UserRoleVO> getRoles()
  {
    return UserGroupCache.getRoles();
  }

  public static UserRoleVO[] getFilteredRoles()
  {
    List<UserRoleVO> filtered = new ArrayList<UserRoleVO>();
    for (UserRoleVO role : getRoles())
    {
      if (!role.isOldGlobalRole())
        filtered.add(role);
    }
    return filtered.toArray(new UserRoleVO[0]);
  }
  public static List<String> getRoleNames()
  {
    List<String> names = new ArrayList<String>();
    for (UserRoleVO role : getRoles())
      names.add(role.getName());
    return names;
  }

  public static UserRoleVO getRoleByName(String name)
  {
    for (UserRoleVO role : getRoles())
    {
      if (role.getName().equals(name))
        return role;
    }
    return null;
  }

  public static List<String> asConsolidatedNewNames(List<String> roles)
  {
    List<String> consolidated = new ArrayList<String>();
    for (String role : roles)
    {
      String newRole = UserRoleVO.toNewName(role);
      if (!consolidated.contains(newRole))
        consolidated.add(newRole);
    }
    return consolidated;
  }

  public static void add(UserRoleVO role)
  {
    UserGroupCache.getRoles().add(role);
    Collections.sort(UserGroupCache.getRoles());
    syncList("rolesList");
  }

  public static void remove(UserRoleVO role)
  {
    UserGroupCache.remove(role);
    syncList("rolesList");
  }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.list.SortableList#retrieveItems()
   */
  public DataModel<ListItem> retrieveItems()
  {
    try
    {
      return new ListDataModel<ListItem>(convertUserRoles(getRoles()));
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }

  protected List<ListItem> convertUserRoles(List<UserRoleVO> roles)
  {
    List<ListItem> rowList = new ArrayList<ListItem>();

    for (UserRoleVO role : roles)
    {
      RoleItem item = new RoleItem(role);
      rowList.add(item);
    }

    return rowList;
  }

  /**
   * Get a list of SelectItems populated from an array of user roles.
   *
   * @param roles the roles to include in the list
   * @param firstItem the message for an optional first item (null for none)
   * @return list of SelectItems
   */
  public static List<SelectItem> getRoleSelectItems(UserRoleVO[] roles, String firstItem)
  {
    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    if (firstItem != null)
      selectItems.add(new SelectItem("0", firstItem));
    for (UserRoleVO userRole : roles)
    {
      selectItems.add(new SelectItem(userRole.getId().toString(), userRole.getName()));
    }

    return selectItems;
  }

  @Override
  public boolean isSortable()
  {
    return !UserGroupCache.getRoles().isEmpty();
  }
}
