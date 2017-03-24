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
package com.centurylink.mdw.service.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.UserServices;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Users")
@Api("MDW users")
public class Users extends JsonRestService {

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.USER_ADMIN);
        return roles;
    }


    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.User;
    }

    /**
     * Retrieve a specific user or a page of users.
     */
    @Override
    @Path("/{cuid}")
    @ApiOperation(value="Retrieve a specific user or a page of users",
        notes="If cuid is not present, returns a page of users; if Find is present, searches by pattern.",
        response=User.class, responseContainer="List")
    @ApiImplicitParams({
        @ApiImplicitParam(name="find", paramType="query", dataType="string")})
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        Map<String,String> parameters = getParameters(headers);
        UserServices userServices = ServiceLocator.getUserServices();
        try {
            String userId = parameters.get("id");
            if (userId == null)
                userId = parameters.get("cuid");
            if (userId == null) // use request path
                userId = getSegment(path, 1);
            if (userId != null) {
                boolean oldStyle = "true".equals(parameters.get("withRoles")); // compatibility for old-style common roles
                return userServices.getUser(userId).getJsonWithRoles(oldStyle);
            }
            else {
                Query query = getQuery(path, headers);
                if (query.getFind() != null)
                    return userServices.findUsers(query.getFind()).getJson();
                else
                    return userServices.getUsers(query.getStart(), query.getMax()).getJson();
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }


    /**
     * For create (creating a new user, or creating a new user/workgroup or user/role relationship).
     */
    @Override
    @Path("/{cuid}/rel/{relId}")
    @ApiOperation(value="Create a user or add existing user to a workgroup or role",
        notes="If rel/{relId} is present, user is added to workgroup or role.", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Workgroup", paramType="body", dataType="com.centurylink.mdw.model.user.User")})
    public JSONObject post(String path, JSONObject content, Map<String, String> headers)
    throws ServiceException, JSONException {
        String cuid = getSegment(path, 1);
        String rel = getSegment(path, 2);

        UserServices userServices = ServiceLocator.getUserServices();
        try {
            if (rel == null) {
                User existing = userServices.getUsers().get(cuid);
                if (existing != null)
                    throw new ServiceException(HTTP_409_CONFLICT, "User ID already exists: " + cuid);
                User user = new User(content);
                userServices.createUser(user);
            }
            else if (rel.equals("workgroups")) {
                String group = getSegment(path, 3);
                userServices.addUserToWorkgroup(cuid, group);
            }
            else if (rel.equals("roles")) {
                String role = getSegment(path, 3);
                userServices.addUserToRole(cuid, role);
            }
            else {
                String msg = "Unsupported relationship for user " + cuid + ": " + rel;
                throw new ServiceException(HTTP_400_BAD_REQUEST, msg);
            }
            return null;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(HTTP_500_INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    /**
     * For update.
     */
    @Override
    @Path("/{cuid}")
    @ApiOperation(value="Update a user", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="User", paramType="body", required=true, dataType="com.centurylink.mdw.model.user.User")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {

        UserServices userServices = ServiceLocator.getUserServices();
        User user = new User(content);
        String cuid = getSegment(path, 1);
        if (cuid == null)
            throw new ServiceException(HTTP_400_BAD_REQUEST, "Missing path segment: {cuid}");
        try {
            User existing = userServices.getUser(cuid);
            if (existing == null)
                throw new ServiceException(HTTP_404_NOT_FOUND, "User not found: " + cuid);
            // update
            user.setId(existing.getId());
            userServices.updateUser(user);
            return null;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(HTTP_500_INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    /**
     * Delete a user or a user/group, user/role relationship.
     */
    @Path("/{cuid}/rel/{relId}")
    @ApiOperation(value="Delete a user or remove a user from a workgroup or role",
        notes="If rel/{relId} is present, user is removed from workgroup or role.", response=StatusMessage.class)
    public JSONObject delete(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        String cuid = getSegment(path, 1);
        String rel = getSegment(path, 2);

        UserServices userServices = ServiceLocator.getUserServices();
        try {
            if (rel == null) {
                userServices.deleteUser(cuid);
            }
            else if (rel.equals("workgroups")) {
                String group = getSegment(path, 3);
                userServices.removeUserFromWorkgroup(cuid, group);
            }
            else if (rel.equals("roles")) {
                String role = getSegment(path, 3);
                userServices.removeUserFromRole(cuid, role);
            }
        }
        catch (DataAccessException ex) {
            throw new ServiceException(HTTP_500_INTERNAL_ERROR, ex.getMessage(), ex);
        }
        return null;
    }
}