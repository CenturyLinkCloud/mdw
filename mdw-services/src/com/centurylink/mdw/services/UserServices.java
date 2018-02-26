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

    public List<User> getWorkgroupUsers(List<String> groups) throws DataAccessException;
    public List<String> getWorkgroupEmails(List<String> groups) throws DataAccessException;
}
