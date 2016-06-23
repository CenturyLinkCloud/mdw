/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.centurylink.mdw.taskmgr.ui.EditableItem;
import com.centurylink.mdw.taskmgr.ui.roles.Roles;
import com.centurylink.mdw.taskmgr.ui.workgroups.Workgroups;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Wraps a model UserVO instance to provide the list item functionality for dynamically
 * displaying columns according to the layout configuration.
 */
public class UserItem extends ListItem implements EditableItem
{
  public static final String ITEM_BEAN = "userItem";

  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public UserItem()
  {
    _user = FacesVariableUtil.getCurrentUser();
  }

  public UserItem(UserVO user)
  {
    _user = user;
  }

  private UserVO _user;
  public UserVO getUser() { return _user; }

  private UserGroupVO _parentGroup;
  public UserGroupVO getParentGroup() { return _parentGroup; }
  public void setParentGroup(UserGroupVO group) { _parentGroup = group; }

  public Long getId()
  {
    return _user.getId();
  }

  // compatibility
  public String getName()
  {
    if (_user.getName() == null)
      return _user.getCuid();
    else
      return _user.getName();
  }
  public void setName(String name)
  {
    _user.setName(name);
  }

  public String getCuid()
  {
    return _user.getCuid();
  }

  public void setCuid(String cuid)
  {
    _user.setCuid(cuid);
  }

  public String getFullName()
  {
    return _user.getName();
  }

  public String getComment()
  {
    return _user.getName();
  }

  public void setComment(String comment)
  {
    _user.setName(comment);
  }

  public Map<String,String> getPreferences()
  {
    return _user.getAttributes();
  }

  public void setPreferences(Map<String,String> prefs)
  {
    _user.setAttributes(prefs);
  }

  public List<SelectItem> getWorkgroupsForUser() throws CachingException
  {
    UserVO user = getUser();
    UserGroupVO[] userGroups = Workgroups.getGroupsForUser(user.getCuid());

    return Users.getUserGroupSelectItems(userGroups);
  }

  public UserGroupVO[] getWorkgroups()
  {
    return getUser().getWorkgroups();
  }

  public List<SelectItem> getWorkgroupsNotForUser() throws CachingException
  {
    // first, get lists of both assigned groups and active groups
    UserVO user = getUser();
    UserGroupVO[] groups = Workgroups.getGroupsForUser(user.getCuid());
    UserGroupVO[] allGroups = Workgroups.getActiveGroups().toArray(new UserGroupVO[0]);

    //remove assigned groups from list
    UserGroupVO[] nonAssigned = getUnmappedGroups(groups, allGroups);

    return Users.getUserGroupSelectItems(nonAssigned);
    }

  public List<SelectItem> getRolesForUser() throws CachingException
  {
    String group = _parentGroup == null ? UserGroupVO.COMMON_GROUP : _parentGroup.getName();
    return getRolesForUser(group);
  }

  public List<SelectItem> getRolesForUser(String groupName) throws CachingException
  {
    UserVO user = UserGroupCache.getUser(getUser().getCuid());
    UserGroupVO group = user.getUserGroup(groupName);
    List<String> roles = group.getRoles();
    if (roles == null)
      roles = new ArrayList<String>();  // no roles mapped for this group
    roles = Roles.asConsolidatedNewNames(roles);
    return Users.getUserRoleSelectItems(roles);
  }

  public List<SelectItem> getRolesNotForUser() throws CachingException
  {
    String group = _parentGroup == null ? UserGroupVO.COMMON_GROUP : _parentGroup.getName();
    return getRolesNotForUser(group);
  }

  public List<SelectItem> getRolesNotForUser(String groupName) throws CachingException
  {
    UserVO user = UserGroupCache.getUser(getUser().getCuid());
    UserGroupVO group = user.getUserGroup(groupName);
    List<String> roles = group.getRoles();
    if (roles == null)
      roles = new ArrayList<String>();  // no roles mapped for this group
    UserRoleVO[] allRoles = Roles.getFilteredRoles();
    // now subtract the assigned roles from all roles
    List<String> nonAssigned = getUnmappedRoles(roles, allRoles);
    return Users.getUserRoleSelectItems(nonAssigned);
  }

  public List<String> getUnmappedRoles(List<String> userRoles, UserRoleVO[] allRoles)
  {
      ArrayList<String> availableRoles = new ArrayList<String>();
      for (int i=0; i<allRoles.length; i++) {
          boolean found = false;
          if (userRoles!=null) {
        	  found = userRoles.contains(allRoles[i].getName());
          }
          if (!found) availableRoles.add(allRoles[i].getName());
      }
      return availableRoles;
  }


  public UserGroupVO[] getUnmappedGroups(UserGroupVO[] userGroups, UserGroupVO[] allGroups)
  {
      ArrayList<UserGroupVO> unmappedGroups = new ArrayList<UserGroupVO>();
      if (userGroups == null || userGroups.length == 0)
      {
          return allGroups;
      }

      for (int i=0; i<allGroups.length; i++) {
          boolean found = false;
          for (int j=0; j<userGroups.length; j++) {
              if (userGroups[j].getId().longValue() == allGroups[i].getId().longValue()) {
                  found = true;
                  break;
              }
          }
          if (!found) unmappedGroups.add(allGroups[i]);
      }

      UserGroupVO[] availableGroups = new UserGroupVO[unmappedGroups.size()];
      for (int i=0; i<unmappedGroups.size(); i++)
      {
          availableGroups[i] = unmappedGroups.get(i);
      }

      return availableGroups;
  }

  public void saveUserGroupRoles(String[] roleIds)
  {
    try
    {
      Long groupId = _parentGroup == null ? UserGroupVO.COMMON_GROUP_ID : _parentGroup.getId();
      UserManager userMgr = RemoteLocator.getUserManager();
      userMgr.updateUserRoles(getId(), groupId, roleIds);
      UserGroupCache.clear();
      _user = Users.getUser(getId());
      auditLogUserAction(Action.Change, Entity.User, _user.getId(), _user.getCuid() + " (Roles)");
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  public void setGroups(String[] groupIds)
  {
    List<UserGroupVO> groups = new ArrayList<UserGroupVO>();
    for (String groupId : groupIds)
    {
      groups.add(Workgroups.getGroup(Long.parseLong(groupId)));
    }
    getUser().setGroups(groups);
  }

  public void saveUserWorkgroups(String[] wgIds) throws UIException
  {
    try
    {
      UserManager userMgr = RemoteLocator.getUserManager();
      userMgr.updateUserGroups(getCuid(), wgIds);
      UserGroupCache.clear();
      _user = Users.getUser(getId());
      auditLogUserAction(Action.Change, Entity.User, _user.getId(), _user.getCuid() + " (Workgroups)");
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
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
      userMgr.deleteUser(getId());
      Users.remove(_user);
      auditLogUserAction(Action.Delete, Entity.User, _user.getId(), _user.getCuid());
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
        if (Users.getUserByCuid(getCuid()) != null)
          throw new UIException("User ID already exists: " + getCuid());
        if (Users.getUserByName(getName()) != null)
          throw new UIException("User Name already exists: " + getName());
        userMgr.addUser(_user);
        auditLogUserAction(Action.Create, Entity.User, _user.getId(), _user.getCuid());
      }
      else
      {
        for (UserVO user : Users.getUsers())
        {
          if (getName().equals(user.getName()) && !getCuid().equals(user.getCuid()))
            throw new UIException("User Name already exists: " + getName());  // renaming to duplicate
        }
    	userMgr.updateUser(getId(), getCuid(), getName());
        auditLogUserAction(Action.Change, Entity.User, _user.getId(), _user.getCuid());
      }
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public void savePreferences() throws UIException
  {
    try
    {
      UserManager userMgr = RemoteLocator.getUserManager();
      Map<String,String> prefs = getPreferences();
      userMgr.updateUserPreferences(getId(), prefs);
      _user = Users.getUser(getId());
      _user.setAttributes(prefs);
      auditLogUserAction(Action.Change, Entity.User, _user.getId(), _user.getCuid() + " (Preferences)");
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public void notificationValidator(FacesContext facesContext, UIComponent toValidate, Object value)
      throws ValidatorException
  {
    AuthenticatedUser authUser = (AuthenticatedUser) FacesVariableUtil.getValue("authenticatedUser");
    String valueStr = value.toString();

    String errorMsg = null;
    if (valueStr.equals(AuthenticatedUser.NOTIFICATION_OPTION_EMAIL) && StringHelper.isEmpty(authUser.getEmail()))
      errorMsg = "E-Mail Address must be provided";
    else if (valueStr.equals(AuthenticatedUser.NOTIFICATION_OPTION_TEXT_MESSAGE) || valueStr.equals(AuthenticatedUser.NOTIFICATION_OPTION_PHONE) && StringHelper.isEmpty(authUser.getPhone()))
      errorMsg = "Phone Number must be provided";

    if (errorMsg != null)
    {
      FacesMessage message = new FacesMessage();
      message.setDetail(errorMsg);
      message.setSummary(errorMsg);
      message.setSeverity(FacesMessage.SEVERITY_ERROR);
      facesContext.addMessage(toValidate.getClientId(facesContext), message);
      throw new ValidatorException(message);
    }
  }

  public List<SelectItem> getNotificationOptions()
  {
    List<SelectItem> notificationOptions = new ArrayList<SelectItem>();
    notificationOptions.add(new SelectItem(" "));
    for (String option : AuthenticatedUser.NOTIFICATION_OPTIONS)
    {
      notificationOptions.add(new SelectItem(option));
    }
    return notificationOptions;
  }

  private String _specType;
  public String getSpecType() { return _specType; }
  public void setSpecType(String specType) { _specType = specType; }

  public List<SelectItem> getSpecSelectItems()
  {
    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    if (_specType != null && _specType.equals("Workgroups"))
    {
      selectItems.add(new SelectItem(" "));
      for (UserGroupVO workgroup : Workgroups.getActiveGroups())
        selectItems.add(new SelectItem(workgroup.getName()));
    }
    else if (_specType != null && _specType.equals("Roles"))
    {
      selectItems.add(new SelectItem(" "));
      for (UserRoleVO userRole : Roles.getRoles())
        selectItems.add(new SelectItem(userRole.getName()));
    }

    Collections.sort(selectItems, new Comparator<SelectItem>()
    {
      public int compare(SelectItem si1, SelectItem si2)
      {
        return si1.getLabel().compareTo(si2.getLabel());
      }
    });

    return selectItems;
  }

  private boolean employee = true;
  public boolean isEmployee() { return employee; }
  public void setEmployee(boolean employee) { this.employee = employee; }

  public boolean isEditableByCurrentUser()
  {
    AuthenticatedUser authUser = FacesVariableUtil.getCurrentUser();
    return authUser.isInRoleForAnyGroup(UserRoleVO.USER_ADMIN);
  }

  public void setToCurrentUser(ActionEvent event)
  {
    _user = FacesVariableUtil.getCurrentUser();
  }

}
