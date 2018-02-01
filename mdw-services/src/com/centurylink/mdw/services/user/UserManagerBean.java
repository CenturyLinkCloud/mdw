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
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.user.AuthenticatedUser;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.service.data.user.UserDataAccess;
import com.centurylink.mdw.services.UserException;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.timer.CodeTimer;


public class UserManagerBean implements UserManager {

    private UserDataAccess getUserDAO() {
        DatabaseAccess db = new DatabaseAccess(null);
        return new UserDataAccess(db);
    }

    public User getUser(String userName)
    throws DataAccessException {
        return getUserDAO().getUser(userName);
    }

    public User getUser(Long userId)
    throws UserException, DataAccessException {
        return getUserDAO().getUser(userId);
    }

    public Workgroup getUserGroup(String groupName, boolean loadRolesForUsers)
    throws UserException, DataAccessException {
        UserDataAccess userDAO = getUserDAO();
        Workgroup userGroup = userDAO.getGroup(groupName);
        if (userGroup == null)
            return null;
        userGroup.setRoles(userDAO.getRolesForGroup(userGroup.getId()));
        List<User> users = userDAO.getUsersForGroup(userGroup.getName(),loadRolesForUsers);
        userGroup.setUsers(users.toArray(new User[0]));
        return userGroup;
    }

    public Workgroup getUserGroup(Long groupId, boolean loadRolesForUsers)
    throws UserException, DataAccessException {
        UserDataAccess userDAO = getUserDAO();
        Workgroup userGroup = userDAO.getGroup(groupId);
        if (userGroup == null)
            return null;
        userGroup.setRoles(userDAO.getRolesForGroup(userGroup.getId()));
        List<User> users = userDAO.getUsersForGroup(userGroup.getName(),loadRolesForUsers);
        userGroup.setUsers(users.toArray(new User[0]));
        return userGroup;
    }

    /**
     * Returns the User Information based on the passed in CUID
     *
     * @param pUserGroupName
     * @return UserGroup
     */
    public boolean doesUserBelongToGroup(String pUserName, String pUserGroupName)
    throws UserException, DataAccessException {
        User user = getUserDAO().getUser(pUserName);
        for (Workgroup group : user.getWorkgroups()) {
            if (group.getName().equals(pUserGroupName)) return true;
        }
        return false;
    }

    /**
     * Returns the User Role Information based on the passed in user Role Name
     *
     * @param pUserRoleName
     * @return UserRole
     */
    public Role getUserRole(String pUserRoleName)
    throws UserException, DataAccessException {
        UserDataAccess userDAO = getUserDAO();
        Role userRole = userDAO.getRole(pUserRoleName);
        if (userRole == null)
            return null;
        List<User> users = userDAO.getUsersForRole(pUserRoleName);
        userRole.setUsers(users.toArray(new User[0]));
        return userRole;
    }

    /**
     * Returns the User Role Information based on the passed in user Role Name
     *
     * @return Collection of UserRole
     */
    public List<Role> getUserRoles()
    throws UserException, DataAccessException {
        return getUserDAO().getAllRoles();
    }

    /**
     * Updates the User Groups for the passed in user Id
     *
     * @param pCUID
     * @param pUpdatedGrps
     */
    public void updateUserGroups(String pCUID, String[] pUpdatedGrps)
    throws UserException, DataAccessException {
        CodeTimer timer = new CodeTimer("UserManager.updateUserGroups()", true);
        User usr = this.getUserDAO().getUser(pCUID);
        if (usr == null) {
            timer.stopAndLogTiming("NoUser");
            throw new UserException("User not found. CUID=" + pCUID);
        }
        getUserDAO().updateGroupsForUser(usr.getId(), pUpdatedGrps);
        timer.stopAndLogTiming("");

    }

    public void updateUserRoles(Long userId, Long groupId, String[] pUpdatedRoles)
    throws UserException, DataAccessException {
        CodeTimer timer = new CodeTimer("UserManager.updateUserRoles()", true);
        getUserDAO().updateRolesForUser(userId, groupId, pUpdatedRoles);
        timer.stopAndLogTiming("");
    }

    public void updateUserRoles(String cuid, String groupName, String[] updatedRoles)
    throws UserException, DataAccessException {
        CodeTimer timer = new CodeTimer("UserManager.updateUserRoles()", true);
        Long userId = getUser(cuid).getId();
        Long groupId = groupName.equals(Workgroup.COMMON_GROUP) ? Workgroup.COMMON_GROUP_ID : getUserGroup(groupName, false).getId();
        getUserDAO().updateRolesForUser(userId, groupId, updatedRoles);
        timer.stopAndLogTiming("");
    }

    /**
     * Returns the users belonging to a set of groups.
     */
    public User[] getUsersForGroups(String[] groups)
    throws UserException, DataAccessException {
        try {
            List<User> uniqueUsers = new ArrayList<User>();
            for (String group : groups) {
                User[] users = UserGroupCache.getWorkgroup(group).getUsers();
                for (User check : users) {
                    boolean found = false;
                    for (User user : uniqueUsers) {
                        if (user.getId().equals(check.getId())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        uniqueUsers.add(check);
                }
            }
            return uniqueUsers.toArray(new User[uniqueUsers.size()]);
        }
        catch (CachingException ex) {
            throw new UserException(ex.getMessage(), ex);
        }
    }

    /**
     * Returns the groups a particular user belongs to.
     *
     * @param pCuid the user's CUID
     * @return the workgroups for this user
     */
    public Workgroup[] getGroupsForUser(String pCuid)
    throws UserException, DataAccessException {
        User user = getUserDAO().getUser(pCuid);
        return user.getWorkgroups();
    }

    /**
     * Returns all the user groups.
     *
     * @return the groups
     */
    public List<Workgroup> getUserGroups(boolean includeDeleted)
    throws UserException, DataAccessException {
        UserDataAccess userDAO = getUserDAO();
        List<Workgroup> groups = userDAO.getAllGroups(includeDeleted);
        for (Workgroup group : groups) {
            group.setRoles(userDAO.getRolesForGroup(group.getId()));
         }
        return groups;
    }

    /**
     * Returns the UserVO array that contains info for all the users and the
     * groups they belong to
     *
     * @return Array of UserVO objects
     */
    public User[] getUserVOs()
    throws UserException, DataAccessException {
        CodeTimer timer = new CodeTimer("UserManager.getUserVOs()", true);
        List<User> userList = this.getUserDAO().queryUsers("END_DATE is null", true, -1, -1, "CUID");
        timer.stopAndLogTiming("");
        return userList.toArray(new User[userList.size()]);
    }

    /**
     * Retrieves a list of shallow UserVOs.
     */
    public List<User> getUsers()
    throws UserException, DataAccessException {
        CodeTimer timer = new CodeTimer("UserManager.getUsers()", true);
        List<User> userList = this.getUserDAO().queryUsers("END_DATE is null", false, -1, -1, "NAME");
        timer.stopAndLogTiming("");
        return userList;
    }

    /**
     * Returns a pageful of user list
     *
     * @return Collection of UserVO
     */
    public  List<User> queryUsers(String whereCondition, boolean withGroups, int startIndex, int endIndex, String sortOn)
    throws UserException, DataAccessException {
        CodeTimer timer = new CodeTimer("UserManager.getAllUsers()", true);
        List<User> retUsers = this.getUserDAO().queryUsers(whereCondition, withGroups, startIndex, endIndex, sortOn);
        timer.stopAndLogTiming("");
        return retUsers;
    }

    public int countUsers(String whereCondition)
    throws UserException, DataAccessException {
         return this.getUserDAO().countUsers(whereCondition);
    }

    /**
     * Updates the users associated with a group.
     *
     * @param groupId the ID of the group to be updated
     * @param userIds the IDs of all users to be included in the group
     */
    public void updateGroupUsers(Long groupId, Long[] userIds)
    throws UserException, DataAccessException {
        CodeTimer timer = new CodeTimer("UserManager.updateUserGroupUsers()", true);
        getUserDAO().updateUsersForGroup(groupId, userIds);
        timer.stopAndLogTiming("");
    }

    /**
     * Updates the users associated with a group.
     *
     * @param groupId the ID of the group to be updated
     * @param userCuids the cuids of all users to be included in the group
     */
    public void updateGroupUsers(Long groupId, String[] userCuids)
    throws UserException, DataAccessException {
        CodeTimer timer = new CodeTimer("UserManager.updateUserGroupUsers()", true);
        getUserDAO().updateUsersForGroup(groupId, userCuids);
        timer.stopAndLogTiming("");
    }

    /**
     * Creates a UserGroup
     *
     * @param pUserGroupVO a user group value object
     */
    public void addUserGroup(Workgroup pUserGroupVO)
    throws UserException, DataAccessException {
        CodeTimer timer = new CodeTimer("UserManager.addUserGroup()", true);
        pUserGroupVO.setId(null);
        Long id = getUserDAO().saveGroup(pUserGroupVO);
        pUserGroupVO.setId(id);
        timer.stopAndLogTiming("");
    }

    /**
     * Updates a UserGroup
     *
     * @param pUserGroupId
     * @param pGroupName
     * @param pComment
     */
    public void updateUserGroup(Workgroup pUserGroupVO)
    throws UserException, DataAccessException {
        CodeTimer timer = new CodeTimer("UserManager.updateUserGroup()", true);
        getUserDAO().saveGroup(pUserGroupVO);
        timer.stopAndLogTiming("");
    }

    /**
     * Delete a UserGroup
     *
     * @param pUserGroupId
     */
    public void deleteUserGroup(Long pUserGroupId)
    throws UserException, DataAccessException {
        try {
            getUserDAO().deleteGroup(pUserGroupId);
        }
        catch (Exception ex) {
            throw new UserException(ex.getMessage(), ex);
        }
    }

    /**
     * Creates a User
     *
     * @param pUserGroupVO a user value object
     */
    public void addUser(User pUserVO)
    throws UserException, DataAccessException {
        CodeTimer timer = new CodeTimer("UserManager.addUser()", true);
        pUserVO.setId(null);
        Long id = getUserDAO().saveUser(pUserVO);
        pUserVO.setId(id);
        timer.stopAndLogTiming("");
    }

    /**
     * Updates a User
     *
     * @param pUserId
     * @param pCuid
     * @param pComment
     */
    public void updateUser(Long pUserId, String pCuid, String pName)
    throws UserException, DataAccessException {
        CodeTimer timer = new CodeTimer("UserManager.updateUser()", true);
        User user = this.getUserDAO().getUser(pUserId);
        if (pCuid != null) {
            user.setCuid(pCuid);
        }
        user.setName(pName);
        this.getUserDAO().saveUser(user);
        timer.stopAndLogTiming("");
    }

    /**
     * Delete a User
     *
     * @param pUserId
     */
    public void deleteUser(Long pUserId)
    throws UserException, DataAccessException {
        getUserDAO().deleteUser(pUserId);
    }

    /**
     * Creates a UserRole
     *
     * @param pUserRole a user role object
     */
    public void addUserRole(Role pUserRole)
    throws UserException, DataAccessException {
        CodeTimer timer = new CodeTimer("UserManager.addUserRole()", true);
        pUserRole.setId(null);
        Long id = this.getUserDAO().saveRole(pUserRole);
        pUserRole.setId(id);
        timer.stopAndLogTiming("");
    }

    /**
     * Updates a UserRole
     *
     * @param pUserRoleId
     * @param pRoleName
     * @param pComment
     */
    public void updateUserRole(Role role)
    throws UserException, DataAccessException {
        CodeTimer timer = new CodeTimer("UserManager.updateUserRole()", true);
        this.getUserDAO().saveRole(role);
        timer.stopAndLogTiming("");
    }

    /**
     * Delete a UserRole
     *
     * @param pUserRoleId
     */
    public void deleteUserRole(Long pUserRoleId)
    throws UserException, DataAccessException {
        getUserDAO().deleteRole(pUserRoleId);
    }

    /**
     * Load an Authenticated user from the database.
     * @param cuid the user to be loaded
     * @return prepopulated auth user
     */
    public AuthenticatedUser loadUser(String cuid)
    throws UserException, DataAccessException {
        // load user
        User user = getUser(cuid);
        if (user == null) {
            return null;
        }
        return new AuthenticatedUser(user, getUserPreferences(user.getId()));
    }

    /**
     * Retrieves the user preference name/value pairs.
     * @param userId
     * @return map (empty if no prefs)
     */
    public Map<String,String> getUserPreferences(Long userId)
    throws UserException, DataAccessException {
        Map<String,String> prefsMap = getUserDAO().getUserPreferences(userId);
        return prefsMap;
    }

    /**
     * Update user preferences.
     * @param userId
     * @param preferences must not be null
     */
    public void updateUserPreferences(Long userId, Map<String,String> preferences)
    throws UserException, DataAccessException {
        getUserDAO().updateUserPreferences(userId, preferences);
    }

    /**
     * Finds e-mail addresses for specified groups
     * @param groups
     * @return array of e-mail addresses
     */
    public List<String> getEmailAddressesForGroups(List<String> groups)
    throws DataAccessException, UserException {
      List<String> addresses = new ArrayList<String>();
      String preferredEmail =null;
      for (User user : getUsersForGroups(groups.toArray(new String[0]))) {
          if (!"dev".equalsIgnoreCase(user.getCuid())) {
              user.setAttributes(getUserPreferences(user.getId()));
              preferredEmail = user.getEmail();
              if(!StringHelper.isEmpty(preferredEmail)){
                  addresses.add(preferredEmail);
              }else if (user.getCuid().indexOf('@') > 0){
                  addresses.add(user.getCuid());
              }
              else{
                  addresses.add(user.getCuid() + "@centurylink.com");
              }
          }
      }
      return addresses;
    }

    public List<String> getPublicUserAttributeNames()
    throws DataAccessException, UserException {
        return getUserDAO().getPublicUserAttributeNames();
    }

    public List<String> getWorkgroupAttributeNames()
    throws DataAccessException {
        return getUserDAO().getGroupAttributeNames();
    }

}
