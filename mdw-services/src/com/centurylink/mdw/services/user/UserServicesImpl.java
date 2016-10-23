/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.user;

import java.util.HashMap;
import java.util.List;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.user.RoleList;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.model.user.UserList;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.WorkgroupList;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.service.data.user.UserDataAccess;
import com.centurylink.mdw.services.UserServices;

public class UserServicesImpl implements UserServices {

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
            // add empty attributes
            if (user.getAttributes() == null)
                user.setAttributes(new HashMap<String,String>());
            for (String name : UserGroupCache.getUserAttributeNames()) {
                if (!user.getAttributes().containsKey(name))
                    user.setAttribute(name, null);
                // substitute friendly attribute names
                if (user.getAttributes().containsKey(User.OLD_EMAIL_ADDRESS)) {
                    String oldEmail = user.getAttributes().remove(User.OLD_EMAIL_ADDRESS);
                    if (user.getAttribute(User.EMAIL_ADDRESS) == null)
                        user.setAttribute(User.EMAIL_ADDRESS, oldEmail);
                }
                if (user.getAttributes().containsKey(User.OLD_PHONE_NUMBER)) {
                    String oldPhone = user.getAttributes().remove(User.OLD_PHONE_NUMBER);
                    if (user.getAttribute(User.PHONE_NUMBER) == null)
                        user.setAttribute(User.PHONE_NUMBER, oldPhone);
                }
            }
            return user;
        }
        catch (CachingException ex) {
            throw new DataAccessException("Cannot find user: " + cuid, ex);
        }
    }

    public Workgroup getWorkgroup(String groupName) throws DataAccessException {
        try {
            return UserGroupCache.getWorkgroup(groupName);
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
        DataAccess.getUserDataAccess(new DatabaseAccess(null)).auditLogUserAction(userAction);;
    }

    public void createWorkgroup(Workgroup workgroup) throws DataAccessException {
        workgroup.setId(getUserDAO().saveGroup(workgroup));
        UserGroupCache.set(workgroup);
    }

    public void updateWorkgroup(Workgroup workgroup) throws DataAccessException {
        getUserDAO().saveGroup(workgroup);
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
}
