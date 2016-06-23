/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.workgroups;

import java.util.Date;
import java.util.List;

import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.centurylink.mdw.taskmgr.ui.EditableItem;
import com.centurylink.mdw.taskmgr.ui.user.Users;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Wraps a model UserGroupVO instance to provide the list item functionality for dynamically
 * displaying columns according to the layout configuration.
 */
public class WorkgroupItem extends ListItem implements EditableItem
{
  public static final String ITEM_BEAN = "workgroupItem";

  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public WorkgroupItem()
  {
    _group = new UserGroupVO(null, null, null);
  }

  public WorkgroupItem(UserGroupVO group)
  {
    _group = group;
  }

  private UserGroupVO _group;
  public UserGroupVO getUserGroup() { return _group; }

  public Long getId()
  {
    return _group.getId();
  }

  public String getName()
  {
    return _group.getName();
  }

  public void setName(String name)
  {
    _group.setName(name);
  }

  public String getDescription()
  {
    return getComment();
  }

  public void setDescription(String description)
  {
    setComment(description);
  }

  public String getComment()
  {
    return _group.getDescription();
  }

  public void setComment(String comment)
  {
    _group.setDescription(comment);
  }

  public Date getEndDate()
  {
    if (_group.getEndDate() == null)
      return null;
    return StringHelper.stringToDate(_group.getEndDate());
  }

  public String getParentGroup()
  {
    return _group.getParentGroup();
  }

  public void setParentGroup(String parent)
  {
    _group.setParentGroup(parent);
  }

  public List<SelectItem> getUsersInWorkgroup() throws CachingException
  {
    return Users.getUserSelectItems(Users.getUsersInWorkgroup(getName()), null);
  }

  public List<SelectItem> getUsersNotInWorkgroup() throws CachingException
  {
    return Users.getUserSelectItems(Users.getUsersNotInWorkgroup(getName()), null);
  }

  /**
   * Calls the workflow to save a new array of userIds associated
   * with the wrapped workgroup.
   *
   * @param userIds
   */
  public void saveWorkgroupUsers(Long[] userIds)
  {
    try
    {
      UserManager userMgr = RemoteLocator.getUserManager();
      userMgr.updateGroupUsers(getId(), userIds);
      UserGroupCache.clear();
      _group.setUsers(Users.getUsersInWorkgroup(getName()));
      auditLogUserAction(Action.Change, Entity.Workgroup, _group.getId(), _group.getName() + " (Users)");
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  public void add() throws UIException
  {
    save();
  }

  public void delete() throws UIException
  {
    try
    {
      UserManager userMgr = RemoteLocator.getUserManager();
      userMgr.deleteUserGroup(getId());
      Workgroups.remove(_group);
      auditLogUserAction(Action.Delete, Entity.Workgroup, _group.getId(), _group.getName());
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public void save() throws UIException
  {
    try
    {
      UserManager userMgr = RemoteLocator.getUserManager();
      if (getId() == null)
      {
        if (Workgroups.getGroupByName(getName()) != null)
          throw new UIException("Workgroup already exists: '" + getName() + "'");
        userMgr.addUserGroup(_group);
        Workgroups.add(_group);
        auditLogUserAction(Action.Create, Entity.Workgroup, _group.getId(), _group.getName());
      }
      else
      {
        for (UserGroupVO group : Workgroups.getAllGroups())
        {
          if (getName().equals(group.getName()) && !getId().equals(group.getId()))
            throw new UIException("Group Name already exists: " + getName());  // renaming to duplicate
        }
        userMgr.updateUserGroup(_group);

        UserGroupCache.clear(); // reflect renamed group user mappings

        auditLogUserAction(Action.Change, Entity.Workgroup, _group.getId(), _group.getName());
      }
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public boolean isEditableByCurrentUser()
  {
    if (!_group.isActive())
      return false;

    AuthenticatedUser authUser = FacesVariableUtil.getCurrentUser();
    return authUser.isInRole(UserGroupVO.SITE_ADMIN_GROUP, UserRoleVO.USER_ADMIN)
        || authUser.isInRole(getName(), UserRoleVO.USER_ADMIN);
  }
}
