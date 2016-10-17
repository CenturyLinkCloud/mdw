/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.UserServices;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Roles")
@Api("MDW user roles")
public class Roles extends JsonRestService {

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(UserRoleVO.USER_ADMIN);
        return roles;
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
        response=UserRoleVO.class, responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        UserServices userServices = ServiceLocator.getUserServices();
        Map<String,String> parameters = getParameters(headers);
        try {
            String roleName = parameters.get("name");
            if (roleName == null) // use request path
                roleName = getSegment(path, 1);
            if (roleName != null) {
                return userServices.getRole(roleName).getJson();
            }
            else {
                return userServices.getRoles().getJson();
            }
        }
        catch (Exception ex) {
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
        @ApiImplicitParam(name="Workgroup", paramType="body", dataType="com.centurylink.mdw.model.value.user.UserRoleVO")})
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        String name = getSegment(path, 1);
        String rel = getSegment(path, 2);

        UserServices userServices = ServiceLocator.getUserServices();
        try {
            UserRoleVO existing = userServices.getRoles().get(name);

            if (rel == null) {
                if (existing != null)
                    throw new ServiceException(HTTP_409_CONFLICT, "Role name already exists: " + name);
                UserRoleVO role = new UserRoleVO(content);
                userServices.createRole(role);
            }
            else if (rel.equals("users")) {
                String cuid = getSegment(path, 3);
                UserVO user = UserGroupCache.getUser(cuid);
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
        @ApiImplicitParam(name="Role", paramType="body", required=true, dataType="com.centurylink.mdw.model.value.user.UserRoleVO")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {

        UserServices userServices = ServiceLocator.getUserServices();
        UserRoleVO role = new UserRoleVO(content);
        String name = getSegment(path, 1);
        if (name == null)
            throw new ServiceException(HTTP_400_BAD_REQUEST, "Missing path segment: {name}");
        try {
            UserRoleVO existing = userServices.getRole(name);
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
