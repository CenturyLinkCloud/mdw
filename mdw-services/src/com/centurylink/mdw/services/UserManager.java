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
package com.centurylink.mdw.services;

import java.util.List;
import java.util.Map;

import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.user.AuthenticatedUser;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;

public interface UserManager {

    /**
     * Load the user from database using CUID
     * @param userName CUID of the user
     * @return the user object, or null if the user does not exist
     * @throws DataAccessException when there is database access failure
     */
    public User getUser(String userName)
    throws DataAccessException;

    /**
     * Load the user from database using user ID
     * @param userId database ID of the user
     * @return the user object, or null if no user entry with this ID exists
     * @throws DataAccessException when there is database access failure
     */
    public User getUser(Long userId)
    throws UserException, DataAccessException;

    /**
     * Load the group from database with the give group name.
     * The users in the group is also loaded.
     * @param groupName
     * @param loadRolesForUsers when it is true, load also roles of the users
     *             within the group
     * @return the group object, or null if the group does not exist
     * @throws UserException
     * @throws DataAccessException
     */
    public Workgroup getUserGroup(String groupName, boolean loadRolesForUsers)
    throws UserException, DataAccessException;

    /**
     * Load the group from database with the given group ID
     * The users in the group is also loaded.
     * @param groupName
     * @param loadRolesForUsers when it is true, load also roles of the users
     *             within the group
     * @return the group object, or null if the group does not exist
     * @throws UserException
     * @throws DataAccessException
     */
    public Workgroup getUserGroup(Long groupId, boolean loadRolesForUsers)
    throws UserException, DataAccessException;

    /**
     * Returns true when the user belongs to the group.
     *
     * @param pUserName CUID of the user
     * @param pUserGroupName group name
     * @return true when the user belongs to the group
     */
    public boolean doesUserBelongToGroup(String pUserName, String pUserGroupName)
    throws UserException, DataAccessException;

    /**
     * Load the role object from database for the given role name
     *
     * @param pUserRoleName role name
     * @return UserRole
     */
    public Role getUserRole(String pUserRoleName)
    throws UserException, DataAccessException;

    /**
     * Returns all the roles in the database
     *
     * @return Array of UserRole
     */
    public List<Role> getUserRoles()
    throws UserException, DataAccessException;

    /**
     * Updates the groups to which the given user belongs
     *
     * @param pCUID CUID of the user
     * @param pUpdatedGrps name of all groups the user should be a member
     */
    public void updateUserGroups(String pCUID, String[] pUpdatedGrps)
    throws UserException, DataAccessException;

    /**
     * Update roles of a user for a given group
     * @param userId the ID of the user
     * @param groupId the ID of the group
     * @param pUpdatedRoles names of all roles for the given user in the given group
     * @throws UserException
     * @throws DataAccessException
     */
    public void updateUserRoles(Long userId, Long groupId, String[] pUpdatedRoles)
    throws UserException, DataAccessException;

    /**
     * Update roles of a user for a given group
     * @param cuid the CUID of the user
     * @param groupName the group name
     * @param updatedRoles names of all roles for the given user in the given group
     * @throws UserException
     * @throws DataAccessException
     */
    public void updateUserRoles(String cuid, String groupName, String[] updatedRoles)
    throws UserException, DataAccessException;

    /**
     * Returns the users belonging to a set of groups.
     */
    public User[] getUsersForGroups(String[] groups)
    throws UserException, DataAccessException;

    /**
     * Returns all the groups a particular user belongs to.
     *
     * @param pCuid the user's CUID
     * @return the workgroups for this user
     */
    public Workgroup[] getGroupsForUser(String pCuid)
    throws UserException, DataAccessException;

    /**
     * Returns all the groups.
     *
     * @param includeDeleted when this is true, the groups
     *         that are deleted (end-dated in the database) are also included
     * @return the groups
     */
    public List<Workgroup> getUserGroups(boolean includeDeleted)
    throws UserException, DataAccessException;

    /**
     * Returns all the users in the database.
     * The groups they belong to are loaded in the returned
     * user object.
     *
     * @return Array of UserVO objects
     */
    public User[] getUserVOs()
    throws UserException, DataAccessException;

    /**
     * Retrieves a list of shallow UserVOs.
     */
    public List<User> getUsers()
    throws UserException, DataAccessException;

    /**
     * Query for users
     * @param whereCondition a SQL expression that will
     *         be included after "where". If it is null,
     *         no where clause will be used and the query returns
     *         all entries in database (pagination is still applicable)
     * @param withGroups if true, each returned user
     *         object will include the list of groups the user
     *         is a member of, along with roles within the group
     * @param startIndex start index for pagination. The first row has index 0
     *         if the value passed in is negative, return all users satisfying the query
     *         without pagination
     * @param endIndex end index for pagination; ignored when startIndex is negative
     * @param sortOn the column name on which the sorting is performed
     * @return a list of users matching the query
     * @throws UserException
     * @throws DataAccessException
     */
    public List<User> queryUsers(String whereCondition,
            boolean withGroups, int startIndex, int endIndex, String sortOn)
    throws UserException, DataAccessException;

    /**
     * Return the number of users satisfying the given where
     * clause
     * @param whereCondition
     * @return
     * @throws UserException
     * @throws DataAccessException
     */
    public int countUsers(String whereCondition)
    throws UserException, DataAccessException;

    /**
     * Updates the users associated with a group.
     *
     * @param groupId the ID of the group to be updated
     * @param userIds the IDs of all users to be included in the group
     */
    public void updateGroupUsers(Long groupId, Long[] userIds)
    throws UserException, DataAccessException;

    /**
     * Updates the users associated with a group.
     *
     * @param groupId the ID of the group to be updated
     * @param userCuids the cuids of all users to be included in the group
     */
    public void updateGroupUsers(Long groupId, String[] userCuids)
    throws UserException, DataAccessException;

    /**
     * Creates a new group in the database
     *
     * @param pUserGroupVO a user group value object
     */
    public void addUserGroup(Workgroup pUserGroupVO)
    throws UserException, DataAccessException;

    /**
     * Update a group's name, description and parent group ID
     * @param pUserGroupVO
     * @throws UserException
     * @throws DataAccessException
     */
    public void updateUserGroup(Workgroup pUserGroupVO)
    throws UserException, DataAccessException;

    /**
     * Delete a group
     *
     * @param pUserGroupId
     */
    public void deleteUserGroup(Long pUserGroupId)
    throws UserException, DataAccessException;

    /**
     * Create a new user
     *
     * @param pUserGroupVO a user value object
     */
    public void addUser(User pUserVO)
    throws UserException, DataAccessException;

    /**
     * Updates a user's CUID and displayed name
     *
     * @param pUserId the ID of the user to be updated
     * @param pCuid the CUID of the user to be set
     * @param pName the displayed name of the user to be set
     */
    public void updateUser(Long pUserId, String pCuid, String pName)
    throws UserException, DataAccessException;

    /**
     * Delete a user
     *
     * @param pUserId the ID of the user to be deleted
     */
    public void deleteUser(Long pUserId)
    throws UserException, DataAccessException;

    /**
     * Creates a new role
     *
     * @param pUserRole a role object to be persisted
     */
    public void addUserRole(Role pUserRole)
    throws UserException, DataAccessException;

    /**
     * Updates a role. Name and description can be changed.
     *
     * @param role content to be updated. The ID field, is used
     *         to identify which role to update, and name and description
     *         are to be modified.
     */
    public void updateUserRole(Role role)
    throws UserException, DataAccessException;

    /**
     * Delete a role
     *
     * @param pUserRoleId the ID of the role to be deleted
     */
    public void deleteUserRole(Long pUserRoleId)
    throws UserException, DataAccessException;

    /**
     * Load an authenticated user from database,
     * along with preferences.
     * @param cuid the user to be loaded
     * @return authenticated user object
     */
    public AuthenticatedUser loadUser(String cuid)
    throws UserException, DataAccessException;

    /**
     * Retrieves the user preference name/value pairs.
     * @param userId
     * @return map (empty if no prefs)
     */
    public Map<String,String> getUserPreferences(Long userId)
    throws UserException, DataAccessException;

    /**
     * Update user preferences.
     * User preferences are stored as attributes.
     * @param userId
     * @param preferences must not be null
     */
    public void updateUserPreferences(Long userId, Map<String,String> preferences)
    throws UserException, DataAccessException;

    /**
     * Finds e-mail addresses for all users in the specified groups.
     * An email address is composed of user's cuid appended
     * with "@centurylink.com"
     * @param groups
     * @return array of e-mail addresses
     */
    public List<String> getEmailAddressesForGroups(String[] groups)
    throws DataAccessException, UserException;

    /**
     * Returns the set of unique user attributes.
     * Public is defined as those whose attribute name does not contain ':'.
     */
    public List<String> getPublicUserAttributeNames()
    throws DataAccessException, UserException;

}
