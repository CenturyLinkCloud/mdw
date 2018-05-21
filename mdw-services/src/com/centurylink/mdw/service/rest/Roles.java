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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.UserServices;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Roles")
@Api("MDW user roles")
public class Roles extends JsonRestService {

    @Override
    public List<String> getRoles(String path, String method) {
        if (method.equals("GET")) {
            List<String> roles = new ArrayList<>();
            if (UserGroupCache.getRole(Role.ASSET_VIEW) != null) {
                roles.add(Role.USER_VIEW);
                roles.add(Role.USER_ADMIN);
                roles.add(Workgroup.SITE_ADMIN_GROUP);
            }
            return roles;
        }
        else {
            List<String> roles = super.getRoles(path);
            roles.add(Role.USER_ADMIN);
            return roles;
        }
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Role;
    }

    /**
     * Retrieve a user role or the list of all roles.
     */
    @Override
    @Path("/{roleName}")
    @ApiOperation(value="Retrieve a role or all roles",
        notes="If roleName is not present, returns all roles.",
        response=Role.class, responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        UserServices userServices = ServiceLocator.getUserServices();
        Map<String,String> parameters = getParameters(headers);
        try {
            String roleName = parameters.get("name");
            if (roleName == null) // use request path
                roleName = getSegment(path, 1);
            if (roleName != null) {
                Role role = userServices.getRole(roleName);
                if (role == null)
                    throw new ServiceException(HTTP_404_NOT_FOUND, "Role not found: " + roleName);
                return role.getJson();
            }
            else {
                return userServices.getRoles().getJson();
            }
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * For creating a new role or a new role/user relationship.
     */
    @Override
    @Path("/{roleName}/users/{cuid}")
    @ApiOperation(value="Create a role or add a user to an existing role",
        notes="If users/{cuid} is present, user is added to role.", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Workgroup", paramType="body", dataType="com.centurylink.mdw.model.user.Role")})
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        String name = getSegment(path, 1);
        String rel = getSegment(path, 2);

        UserServices userServices = ServiceLocator.getUserServices();
        try {
            Role existing = userServices.getRoles().get(name);

            if (rel == null) {
                if (existing != null)
                    throw new ServiceException(HTTP_409_CONFLICT, "Role name already exists: " + name);
                Role role = new Role(content);
                userServices.createRole(role);
            }
            else if (rel.equals("users")) {
                String cuid = getSegment(path, 3);
                User user = UserGroupCache.getUser(cuid);
                if (user == null) {
                    throw new CachingException("Cannot find user: " + cuid);
                }
                if (user.hasRole(name))  // in case added elsewhere
                    throw new ServiceException(HTTP_409_CONFLICT, "User " + cuid + " already has role " + name);

                userServices.addUserToRole(cuid, name);
            }
            else {
                String msg = "Unsupported relationship for role " + name + ": " + rel;
                throw new ServiceException(HTTP_400_BAD_REQUEST, msg);
            }
            return null;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(HTTP_500_INTERNAL_ERROR, ex.getMessage(), ex);
        }
        catch (CachingException ex) {
            throw new ServiceException(HTTP_500_INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    /**
     * For update.
     */
    @Override
    @Path("/{roleName}")
    @ApiOperation(value="Update a role", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Role", paramType="body", required=true, dataType="com.centurylink.mdw.model.user.Role")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {

        UserServices userServices = ServiceLocator.getUserServices();
        Role role = new Role(content);
        String name = getSegment(path, 1);
        if (name == null)
            throw new ServiceException(HTTP_400_BAD_REQUEST, "Missing path segment: {name}");
        try {
            Role existing = userServices.getRole(name);
            if (existing == null)
                throw new ServiceException(HTTP_404_NOT_FOUND, "Role not found: " + name);
            // update
            role.setId(existing.getId());
            userServices.updateRole(role);
            return null;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(HTTP_500_INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    /**
     * Delete workgroup or remove user from group.
     */
    @Path("/{roleName}/users/{cuid}")
    @ApiOperation(value="Delete a role or remove a user from a role",
        notes="If users/{cuid} is present, user is removed from role.", response=StatusMessage.class)
    public JSONObject delete(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        String name = getSegment(path, 1);
        String rel = getSegment(path, 2);

        UserServices userServices = ServiceLocator.getUserServices();
        try {
            if (rel == null) {
                userServices.deleteRole(name);
              }
              else if (rel.equals("users")) {
                  String cuid = getSegment(path, 3);
                  userServices.removeUserFromRole(cuid, name);
              }
        }
        catch (DataAccessException ex) {
            throw new ServiceException(HTTP_500_INTERNAL_ERROR, ex.getMessage(), ex);
        }
        return null;
    }
}
