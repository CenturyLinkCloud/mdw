/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.model.value.user.RoleList;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserList;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.model.value.user.WorkgroupList;

public interface UserServices {

    public WorkgroupList getWorkgroups() throws DataAccessException;
    public UserGroupVO getWorkgroup(String name) throws DataAccessException;
    public void createWorkgroup(UserGroupVO workgroup) throws DataAccessException;
    public void updateWorkgroup(UserGroupVO workgroup) throws DataAccessException;
    public void deleteWorkgroup(String name) throws DataAccessException;

    public UserList getUsers() throws DataAccessException;
    /**
     * Retrieves one page of users.
     */
    public UserList getUsers(int start, int pageSize) throws DataAccessException;
    /**
     * Find users whose first or last name begins with prefix.
     */
    public UserList findUsers(String prefix) throws DataAccessException;
    public UserVO getUser(String cuid) throws DataAccessException;
    public void createUser(UserVO user) throws DataAccessException;
    public void updateUser(UserVO user) throws DataAccessException;
    public void deleteUser(String cuid) throws DataAccessException;
    public void addUserToWorkgroup(String cuid, String group) throws DataAccessException;
    public void removeUserFromWorkgroup(String cuid, String group) throws DataAccessException;
    public void addUserToRole(String cuid, String role) throws DataAccessException;
    public void removeUserFromRole(String cuid, String role) throws DataAccessException;


    public RoleList getRoles() throws DataAccessException;
    public UserRoleVO getRole(String name) throws DataAccessException;
    public void createRole(UserRoleVO role) throws DataAccessException;
    public void updateRole(UserRoleVO role) throws DataAccessException;
    public void deleteRole(String name) throws DataAccessException;

    public void auditLog(UserActionVO userAction) throws DataAccessException;

}
