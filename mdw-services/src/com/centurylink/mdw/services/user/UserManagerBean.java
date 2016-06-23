/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.timer.CodeTimer;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.services.UserException;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.services.dao.user.UserDAO;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;


public class UserManagerBean implements UserManager {

    private UserDAO getUserDAO() {
    	DatabaseAccess db = new DatabaseAccess(null);
    	return new UserDAO(db);
    }

    public UserVO getUser(String userName)
    throws DataAccessException {
        return getUserDAO().getUser(userName);
    }

    public UserVO getUser(Long userId)
    throws UserException, DataAccessException {
        return getUserDAO().getUser(userId);
    }

    public UserGroupVO getUserGroup(String groupName, boolean loadRolesForUsers)
    throws UserException, DataAccessException {
        UserDAO userDAO = getUserDAO();
        UserGroupVO userGroup = userDAO.getGroup(groupName);
        if (userGroup == null)
            return null;
        userGroup.setRoles(userDAO.getRolesForGroup(userGroup.getId()));
        List<UserVO> users = userDAO.getUsersForGroup(userGroup.getName(),loadRolesForUsers);
        userGroup.setUsers(users.toArray(new UserVO[0]));
        return userGroup;
    }

    public UserGroupVO getUserGroup(Long groupId, boolean loadRolesForUsers)
    throws UserException, DataAccessException {
        UserDAO userDAO = getUserDAO();
        UserGroupVO userGroup = userDAO.getGroup(groupId);
        if (userGroup == null)
            return null;
        userGroup.setRoles(userDAO.getRolesForGroup(userGroup.getId()));
        List<UserVO> users = userDAO.getUsersForGroup(userGroup.getName(),loadRolesForUsers);
        userGroup.setUsers(users.toArray(new UserVO[0]));
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
    	UserVO user = getUserDAO().getUser(pUserName);
    	for (UserGroupVO group : user.getWorkgroups()) {
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
    public UserRoleVO getUserRole(String pUserRoleName)
    throws UserException, DataAccessException {
        UserDAO userDAO = getUserDAO();
        UserRoleVO userRole = userDAO.getRole(pUserRoleName);
        if (userRole == null)
            return null;
        List<UserVO> users = userDAO.getUsersForRole(pUserRoleName);
        userRole.setUsers(users.toArray(new UserVO[0]));
        return userRole;
    }

    /**
     * Returns the User Role Information based on the passed in user Role Name
     *
     * @return Collection of UserRole
     */
    public List<UserRoleVO> getUserRoles()
    throws UserException, DataAccessException {
    	return getUserDAO().getAllRoles();
    }

    /**
     * Returns all the roles that are mapped to the task action.
     *
     * @param pTaskActionId
     * @return array of UserRole objects
     */
    public UserRoleVO[] getUserRolesForTaskAction(Long pTaskActionId)
    throws UserException, DataAccessException {
    	List<UserRoleVO> r = getUserDAO().getRolesForAction(pTaskActionId);
        return r.toArray(new UserRoleVO[r.size()]);
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
        UserVO usr = this.getUserDAO().getUser(pCUID);
        if (usr == null) {
            timer.stopAndLogTiming("NoUser");
            throw new UserException("User not found. CUID=" + pCUID);
        }
        getUserDAO().updateGroupsForUser(usr.getId(), pUpdatedGrps);
        timer.stopAndLogTiming("");

    }

    /**
     * Updates the User roles for the passed in user Id
     *
     * @param pCUID
     * @param pUpdatedGrps
     */
    @Deprecated
    public void updateUserRoles(String pCUID, String[] pUpdatedRoles)
    throws UserException, DataAccessException {
        CodeTimer timer = new CodeTimer("UserManager.updateUserRoles()", true);
        UserVO usr = this.getUserDAO().getUser(pCUID);
        if (usr == null) {
            timer.stopAndLogTiming("NoUser");
            throw new UserException("User with CUID does not exists. CUID=" + pCUID);
        }
        getUserDAO().updateRolesForUser(usr.getId(), UserGroupVO.COMMON_GROUP_ID, pUpdatedRoles);
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
        Long groupId = groupName.equals(UserGroupVO.COMMON_GROUP) ? UserGroupVO.COMMON_GROUP_ID : getUserGroup(groupName, false).getId();
        getUserDAO().updateRolesForUser(userId, groupId, updatedRoles);
        timer.stopAndLogTiming("");
    }

    /**
     * Returns the users belonging to a set of groups.
     */
    public UserVO[] getUsersForGroups(String[] groups)
    throws UserException, DataAccessException {
        try {
            List<UserVO> uniqueUsers = new ArrayList<UserVO>();
            for (String group : groups) {
                UserVO[] users = UserGroupCache.getWorkgroup(group).getUsers();
                for (UserVO check : users) {
                    boolean found = false;
                    for (UserVO user : uniqueUsers) {
                        if (user.getId().equals(check.getId())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        uniqueUsers.add(check);
                }
            }
            return uniqueUsers.toArray(new UserVO[uniqueUsers.size()]);
        }
        catch (CachingException ex) {
            throw new UserException(ex.getMessage(), ex);
        }
    }

    /**
     * Returns the users belonging to a role
     */
    @Deprecated
    public List<UserVO> getUsersForRole(String roleName)
    throws UserException, DataAccessException {
    	UserDAO userDAO = getUserDAO();
    	return userDAO.getUsersForRole(roleName);
    }

    /**
     * Returns the groups a particular user belongs to.
     *
     * @param pCuid the user's CUID
     * @return the workgroups for this user
     */
    public UserGroupVO[] getGroupsForUser(String pCuid)
    throws UserException, DataAccessException {
    	UserVO user = getUserDAO().getUser(pCuid);
    	return user.getWorkgroups();
    }

    /**
     * Returns all the user groups.
     *
     * @return the groups
     */
    public List<UserGroupVO> getUserGroups(boolean includeDeleted)
    throws UserException, DataAccessException {
        UserDAO userDAO = getUserDAO();
        List<UserGroupVO> groups = userDAO.getAllGroups(includeDeleted);
    	for (UserGroupVO group : groups) {
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
    public UserVO[] getUserVOs()
    throws UserException, DataAccessException {
        CodeTimer timer = new CodeTimer("UserManager.getUserVOs()", true);
        List<UserVO> userList = this.getUserDAO().queryUsers("END_DATE is null", true, -1, -1, "CUID");
        timer.stopAndLogTiming("");
        return userList.toArray(new UserVO[userList.size()]);
    }

    /**
     * Retrieves a list of shallow UserVOs.
     */
    public List<UserVO> getUsers()
    throws UserException, DataAccessException {
        CodeTimer timer = new CodeTimer("UserManager.getUsers()", true);
        List<UserVO> userList = this.getUserDAO().queryUsers("END_DATE is null", false, -1, -1, "NAME");
        timer.stopAndLogTiming("");
        return userList;
    }

    /**
     * Returns a pageful of user list
     *
     * @return Collection of UserVO
     */
    public  List<UserVO> queryUsers(String whereCondition, boolean withGroups, int startIndex, int endIndex, String sortOn)
    throws UserException, DataAccessException {
        CodeTimer timer = new CodeTimer("UserManager.getAllUsers()", true);
        List<UserVO> retUsers = this.getUserDAO().queryUsers(whereCondition, withGroups, startIndex, endIndex, sortOn);
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
    public void addUserGroup(UserGroupVO pUserGroupVO)
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
    public void updateUserGroup(UserGroupVO pUserGroupVO)
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
    public void addUser(UserVO pUserVO)
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
        UserVO user = this.getUserDAO().getUser(pUserId);
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
    public void addUserRole(UserRoleVO pUserRole)
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
    public void updateUserRole(UserRoleVO role)
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

    private TaskAction[] getTaskActionsForUserAndUserGroup(String userId)
    throws DataAccessException {
        TaskAction[] retActions = null;
        CodeTimer timer = new CodeTimer("TaskManager.getTaskActionsForUserAndUserGroup()", true);
        List<TaskAction> userActions = getUserDAO().getTaskActionsForUser(userId);
        List<TaskAction> userGroupActions = getUserDAO().getTaskActionsForUserGroups(userId);
        Map<Long,TaskAction> tempMap = new HashMap<Long,TaskAction>();
        List<TaskAction> uniqueList = new ArrayList<TaskAction>();
        for (TaskAction ta : userActions) {
        	 if (!tempMap.containsKey(ta.getTaskActionId())) {
                 tempMap.put(ta.getTaskActionId(), ta);
                 uniqueList.add(ta);
             }
        }
        for (TaskAction ta : userGroupActions) {
        	if (!tempMap.containsKey(ta.getTaskActionId())) {
        		tempMap.put(ta.getTaskActionId(), ta);
                uniqueList.add(ta);
            }
        }
        retActions = uniqueList.toArray(new TaskAction[uniqueList.size()]);
        timer.stopAndLogTiming("");
        return retActions;

    }

    /**
     * Load an Authenticated user from the database.
     * @param cuid the user to be loaded
     * @return prepopulated auth user
     */
    public AuthenticatedUser loadUser(String cuid)
    throws UserException, DataAccessException {
        AuthenticatedUser authUser = new AuthenticatedUser();
        // load user
        UserVO user = getUser(cuid);
        if (user == null) {
            return null;
        }
        authUser.setCuid(user.getCuid());
        authUser.setId(user.getId());
        authUser.setName(user.getName());
        authUser.setWorkgroups(user.getWorkgroups());

        // load allowable actions
        if (DataAccess.supportedSchemaVersion<DataAccess.schemaVersion51) {
        	try {
        		TaskAction[] actions = getTaskActionsForUserAndUserGroup(cuid);
        		authUser.setAllowableActions(actions);
        	}
        	catch (Exception ex) {
        		throw new UserException(ex.getMessage(), ex);
        	}
        }

        // load preferences
        authUser.setAttributes(getUserPreferences(user.getId()));

        return authUser;
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
    public List<String> getEmailAddressesForGroups(String[] groups)
    throws DataAccessException, UserException {
      List<String> addresses = new ArrayList<String>();
      String preferredEmail =null;
      for (UserVO user : getUsersForGroups(groups)) {
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
}
