/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.user.RoleList;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.model.user.UserList;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.WorkgroupList;

public interface UserServices {

    public WorkgroupList getWorkgroups() throws DataAccessException;
    public Workgroup getWorkgroup(String name) throws DataAccessException;
    public void createWorkgroup(Workgroup workgroup) throws DataAccessException;
    public void updateWorkgroup(Workgroup workgroup) throws DataAccessException;
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
    /**
     * Find users who belong to a list of workgroups.
     */
    public UserList findWorkgroupUsers(String[] workgroups, String prefix) throws DataAccessException;
    public User getUser(String cuid) throws DataAccessException;
    public void createUser(User user) throws DataAccessException;
    public void updateUser(User user) throws DataAccessException;
    public void deleteUser(String cuid) throws DataAccessException;
    public void addUserToWorkgroup(String cuid, String group) throws DataAccessException;
    public void removeUserFromWorkgroup(String cuid, String group) throws DataAccessException;
    public void addUserToRole(String cuid, String role) throws DataAccessException;
    public void removeUserFromRole(String cuid, String role) throws DataAccessException;


    public RoleList getRoles() throws DataAccessException;
    public Role getRole(String name) throws DataAccessException;
    public void createRole(Role role) throws DataAccessException;
    public void updateRole(Role role) throws DataAccessException;
    public void deleteRole(String name) throws DataAccessException;

    public void auditLog(UserAction userAction) throws DataAccessException;

}
