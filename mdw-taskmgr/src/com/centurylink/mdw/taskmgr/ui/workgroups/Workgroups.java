/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.workgroups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.layout.ViewUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

/**
 * UI List class for workgroups admin.  Also maintains a static
 * list of groups reference data loaded from the workflow.
 */
public class Workgroups extends SortableList
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  /**
   *  Needs a no-arg constructor since Users is a dropdown lister
   *  in addition to being a SortableList implementation.
   */
  public Workgroups() throws UIException
  {
    super(ViewUI.getInstance().getListUI("workgroupsList"));
  }

  public static List<UserGroupVO> getAllGroups()
  {
    return UserGroupCache.getWorkgroups();
  }

  public static List<UserGroupVO> getActiveGroups()
  {
    List<UserGroupVO> active = new ArrayList<UserGroupVO>();
    for (UserGroupVO group : getAllGroups())
    {
      if (group.isActive())
        active.add(group);
    }
    return active;
  }

  public Workgroups(ListUI listUI)
  {
    super(listUI);
  }


  public static void add(UserGroupVO group)
  {
    UserGroupCache.getWorkgroups().add(group);
    Collections.sort(UserGroupCache.getWorkgroups());
    syncList("workgroupsList");
  }
  public static void remove(UserGroupVO group)
  {
    group.setEndDate(StringHelper.dateToString(new Date()));
    UserGroupCache.remove(group);
    syncList("workgroupsList");
  }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.list.SortableList#retrieveItems()
   */
  public DataModel<ListItem> retrieveItems()
  {
    try
    {
      return new ListDataModel<ListItem>(convertUserGroupVOs(getAllGroups()));
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }

  public static UserGroupVO[] getGroupsForUser(String cuid) throws CachingException
  {
    UserVO user = UserGroupCache.getUser(cuid);
    return user.getWorkgroups();
  }

  /**
   * Converts a collection of workgroups.
   */
  protected List<ListItem> convertUserGroupVOs(List<UserGroupVO> groups)
  {
    List<ListItem> rowList = new ArrayList<ListItem>();

    for (UserGroupVO group : groups)
    {
      WorkgroupItem item = new WorkgroupItem(group);
      rowList.add(item);
    }

    return rowList;
  }

  public static UserGroupVO getGroup(Long id)
  {
    for (UserGroupVO group : getAllGroups())
    {
      if (group.getId().equals(id))
        return group;
    }
    return null;
  }

  public static UserGroupVO getGroupByName(String name)
  {
    for (UserGroupVO group : getAllGroups())
    {
      if (group.getName().equals(name))
        return group;
    }
    return null;
  }

  /**
   * Get a list of SelectItems populated from an array of group VOs.
   *
   * @param groups the group VOs to include in the list
   * @param firstItem the message for an optional first item (null for none)
   * @return list of SelectItems
   */
  public static List<SelectItem> getWorkgroupSelectItems(List<UserGroupVO> groups, String firstItem)
  {
    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    if (firstItem != null)
      selectItems.add(new SelectItem("0", firstItem));
    for (UserGroupVO group : groups)
      selectItems.add(new SelectItem(group.getId().toString(), group.getName()));

    return selectItems;
  }

  /**
   * Get a list of SelectItems populated from an array of active group VOs.
   *
   * @return list of SelectItems
   */
  public List<SelectItem> getActiveGroupSelectItems()
  {

    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    for (UserGroupVO group : getActiveGroups())
      selectItems.add(new SelectItem(group.getId().toString(), group.getName()));

    return selectItems;
  }

  @Override
  public boolean isSortable()
  {
    return !UserGroupCache.getWorkgroups().isEmpty();
  }
}
