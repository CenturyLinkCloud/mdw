/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.roles;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.taskmgr.ui.EditableItem;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Wraps a model UserRole instance to provide the list item functionality for dynamically
 * displaying columns according to the layout configuration.
 */
public class RoleItem extends ListItem implements EditableItem
{
  public static final String ITEM_BEAN = "roleItem";
  
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public RoleItem()
  {
    _role = new UserRoleVO();
  }

  public RoleItem(UserRoleVO role)
  {
    _role = role;
  }

  private UserRoleVO _role;
  public UserRoleVO getRole() { return _role; }

  public Long getId()
  {
    return _role.getId();
  }

  public String getName()
  {
    return _role.getName();
  }

  public void setName(String name)
  {
    _role.setName(name);
  }

  public String getComment()
  {
    return _role.getDescription();
  }

  public void setComment(String comment)
  {
    _role.setDescription(comment);
  }
  
  public String getDescription()
  {
    return getComment();
  }
  
  public void setDescription(String description)
  {
    setComment(description);
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
      userMgr.deleteUserRole(getId());
      Roles.remove(_role);
      auditLogUserAction(Action.Delete, Entity.Role, _role.getId(), _role.getName());      
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
        if (Roles.getRoleByName(getName()) != null)
          throw new UIException("Role already exists: '" + getName() + "'");
        userMgr.addUserRole(_role);
        Roles.add(_role);
        auditLogUserAction(Action.Create, Entity.Role, _role.getId(), _role.getName());
      }
      else
      {
        for (UserRoleVO role : Roles.getRoles())
        {
          if (getName().equals(role.getName()) && !getId().equals(role.getId()))
            throw new UIException("Role Name already exists: " + getName());  // renaming to duplicate
        }
        UserRoleVO role = new UserRoleVO();
        role.setId(getId());
        role.setName(getName());
        role.setDescription(getDescription());
        userMgr.updateUserRole(role);
        
        auditLogUserAction(Action.Change, Entity.Role, _role.getId(), _role.getName());
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
    AuthenticatedUser authUser = FacesVariableUtil.getCurrentUser(); 
    return authUser.isInRole(UserGroupVO.SITE_ADMIN_GROUP, UserRoleVO.USER_ADMIN);
  }
  
}
