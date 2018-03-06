/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.services.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.RoleList;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.user.UserList;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.model.user.WorkgroupList;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.service.data.user.UserDataAccess;
import com.centurylink.mdw.services.UserServices;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class UserServicesImpl implements UserServices {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private UserDataAccess getUserDAO() {
        DatabaseAccess db = new DatabaseAccess(null);
        return new UserDataAccess(db);
    }

    public WorkgroupList getWorkgroups() throws DataAccessException {
        List<Workgroup> groups = UserGroupCache.getWorkgroups();
        WorkgroupList groupList = new WorkgroupList(groups);
        groupList.setRetrieveDate(DatabaseAccess.getDbDate());
        return groupList;
    }

    public RoleList getRoles() throws DataAccessException {
        List<Role> roles = UserGroupCache.getRoles();
        RoleList roleList = new RoleList(roles);
        roleList.setRetrieveDate(DatabaseAccess.getDbDate());
        return roleList;
    }

    public UserList getUsers() throws DataAccessException {
        List<User> users = UserGroupCache.getUsers();
        UserList userList = new UserList(users);
        userList.setRetrieveDate(DatabaseAccess.getDbDate());
        return userList;
    }

    public UserList getUsers(int start, int pageSize) throws DataAccessException {
        List<User> users = UserGroupCache.getUsers(start, pageSize);
        UserList userList = new UserList(users);
        userList.setRetrieveDate(DatabaseAccess.getDbDate());
        userList.setTotal(UserGroupCache.getTotalUsers());
        return userList;
    }

    public UserList findUsers(String prefix) throws DataAccessException {
        List<User> users = UserGroupCache.findUsers(prefix);
        return new UserList(users);
    }

    public UserList findWorkgroupUsers(String[] workgroups, String prefix) throws DataAccessException {
        try {
            List<User> users = UserGroupCache.findUsers(workgroups, prefix);
            return new UserList(users);
        }
        catch (CachingException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * Does not include non-public attributes.
     * Includes empty values for all public attributes.
     */
    public User getUser(String cuid) throws DataAccessException {
        try {
            User user = UserGroupCache.getUser(cuid);
            if (user == null)
                return null;
            // add empty attributes
            if (user.getAttributes() == null)
                user.setAttributes(new HashMap<String,String>());
            for (String name : UserGroupCache.getUserAttributeNames()) {
                if (!user.getAttributes().containsKey(name))
                    user.setAttribute(name, null);
                // substitute friendly attribute names
                if (user.getAttributes().containsKey(User.OLD_EMAIL)) {
                    String oldEmail = user.getAttributes().remove(User.OLD_EMAIL);
                    if (user.getAttribute(User.EMAIL) == null)
                        user.setAttribute(User.EMAIL, oldEmail);
                }
                if (user.getAttributes().containsKey(User.OLD_PHONE)) {
                    String oldPhone = user.getAttributes().remove(User.OLD_PHONE);
                    if (user.getAttribute(User.PHONE) == null)
                        user.setAttribute(User.PHONE, oldPhone);
                }
            }
            return user;
        }
        catch (CachingException ex) {
            throw new DataAccessException(ServiceException.NOT_FOUND, "Cannot find user: " + cuid, ex);
        }
    }

    public Workgroup getWorkgroup(String groupName) throws DataAccessException {
        try {
            Workgroup workgroup = UserGroupCache.getWorkgroup(groupName);
            if (workgroup != null) {
                // add empty attributes
                if (workgroup.getAttributes() == null)
                    workgroup.setAttributes(new HashMap<String,String>());
                for (String name : UserGroupCache.getWorkgroupAttributeNames()) {
                    if (!workgroup.getAttributes().containsKey(name))
                        workgroup.setAttribute(name, null);
                }
            }

            return workgroup;
        }
        catch (CachingException ex) {
            throw new DataAccessException("Cannot find workgroup: " + groupName, ex);
        }
    }

    public Role getRole(String roleName) throws DataAccessException {
        try {
            return UserGroupCache.getRole(roleName);
        }
        catch (CachingException ex) {
            throw new DataAccessException("Cannot find role: " + roleName, ex);
        }
    }

    public void auditLog(UserAction userAction) throws DataAccessException {
        getUserDAO().auditLogUserAction(userAction);
    }

    public void createWorkgroup(Workgroup workgroup) throws DataAccessException {
        workgroup.setId(getUserDAO().saveGroup(workgroup));
        getUserDAO().updateGroupAttributes(workgroup.getId(), workgroup.getAttributes());
        UserGroupCache.set(workgroup);
    }

    public void updateWorkgroup(Workgroup workgroup) throws DataAccessException {
        getUserDAO().saveGroup(workgroup);
        getUserDAO().updateGroupAttributes(workgroup.getId(), workgroup.getAttributes());
        UserGroupCache.set(workgroup);
    }

    public void deleteWorkgroup(String name) throws DataAccessException {
        UserDataAccess dao = getUserDAO();
        Workgroup group = dao.getGroup(name);
        if (group == null)
            throw new DataAccessException("Workgroup: " + name + " does not exist");
        dao.deleteGroup(group.getId());
        UserGroupCache.remove(group);
    }

    public void createUser(User user) throws DataAccessException {
        user.setId(getUserDAO().saveUser(user));
        getUserDAO().updateUserAttributes(user.getId(), user.getAttributes());
        UserGroupCache.set(user);
    }

    public void updateUser(User user) throws DataAccessException {
        user.setId(getUserDAO().saveUser(user));
        getUserDAO().updateUserAttributes(user.getId(), user.getAttributes());
        UserGroupCache.set(user);
    }

    public void deleteUser(String cuid) throws DataAccessException {
        UserDataAccess dao = getUserDAO();
        User user = dao.getUser(cuid);
        if (cuid == null)
            throw new DataAccessException("User: " + cuid + " does not exist");
        dao.deleteUser(user.getId());
        UserGroupCache.remove(user);
    }

    public void addUserToWorkgroup(String cuid, String group) throws DataAccessException {
        try {
            getUserDAO().addUserToGroup(cuid, group);
            UserGroupCache.clear();
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public void removeUserFromWorkgroup(String cuid, String group) throws DataAccessException {
        getUserDAO().removeUserFromGroup(cuid, group);
        UserGroupCache.clear();
    }

    public void addUserToRole(String cuid, String role) throws DataAccessException {
        getUserDAO().addUserToRole(cuid, role);
        UserGroupCache.clear();
    }

    public void removeUserFromRole(String cuid, String role) throws DataAccessException {
        getUserDAO().removeUserFromRole(cuid, role);
        UserGroupCache.clear();
    }
    public void createRole(Role role) throws DataAccessException {
        role.setId(getUserDAO().saveRole(role));
        UserGroupCache.set(role);
    }

    public void updateRole(Role role) throws DataAccessException {
        getUserDAO().saveRole(role);
        UserGroupCache.set(role);
    }

    public void deleteRole(String name) throws DataAccessException {
        UserDataAccess dao = getUserDAO();
        Role role = dao.getRole(name);
        if (role == null)
            throw new DataAccessException("Role: " + name + " does not exist");
        dao.deleteRole(role.getId());
        UserGroupCache.remove(role);
    }

    public List<User> getWorkgroupUsers(List<String> groups)
    throws DataAccessException {
        List<User> users = new ArrayList<>();
        for (String group : groups) {
            Workgroup workgroup = getWorkgroup(group);
            for (User user : workgroup.getUsers()) {
                if (!users.contains(user))
                    users.add(user);
            }
        }
        return users;
    }

    public List<String> getWorkgroupEmails(List<String> groups)
    throws DataAccessException {
      List<String> emails = new ArrayList<>();
      for (User user : getWorkgroupUsers(groups)) {
          User deepUser = UserGroupCache.getUser(user.getId());
          if (deepUser == null)
              throw new DataAccessException("User not found: " + user.getCuid());
          String email = deepUser.getEmail();
          if (email != null) {
              if (!emails.contains(email))
                  emails.add(email);
          }
          else {
              logger.warn("No email address found for user: " + user.getCuid());
          }
      }
      return emails;
    }
}
