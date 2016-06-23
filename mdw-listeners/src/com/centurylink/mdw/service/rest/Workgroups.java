/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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
import com.centurylink.mdw.model.value.user.UserGroupVO;
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

@Path("/Workgroups")
@Api("MDW user workgroups service")
public class Workgroups extends JsonRestService {

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(UserRoleVO.USER_ADMIN);
        return roles;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Workgroup;
    }

    /**
     * Retrieve a workgroup or all workgroups.
     */
    @Override
    @Path("/{groupName}")
    @ApiOperation(value="Retrieve a workgroup or all workgroups",
        notes="If groupName is not present, returns all workgroups.",
        response=UserGroupVO.class, responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers)
    throws ServiceException, JSONException {
        UserServices userServices = ServiceLocator.getUserServices();
        try {
            String groupName = getSegment(path, 1);
            if (groupName == null) // check parameter
                groupName = getParameters(headers).get("name");
            if (groupName != null) {
                return userServices.getWorkgroup(groupName).getJson();
            }
            else {
                return userServices.getWorkgroups().getJson();
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * For creating a new workgroup or a new group/user relationship.
     */
    @Override
    @Path("/{groupName}/users/{cuid}")
    @ApiOperation(value="Create a workgroup or add a user to an existing workgroup",
        notes="If users/{cuid} is present, user is added to workgroup.", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Workgroup", paramType="body", dataType="com.centurylink.mdw.model.value.user.UserGroupVO")})
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        String name = getSegment(path, 1);
        String rel = getSegment(path, 2);

        UserServices userServices = ServiceLocator.getUserServices();
        try {
            UserGroupVO existing = userServices.getWorkgroups().get(name);
            if (rel == null) {
                if (existing != null)
                    throw new ServiceException(HTTP_409_CONFLICT, "Workgroup name already exists: " + name);
                UserGroupVO workgroup = new UserGroupVO(content);
                userServices.createWorkgroup(workgroup);
            }
            else if (rel.equals("users")) {
                String cuid = getSegment(path, 3);
                UserVO user = UserGroupCache.getUser(cuid);
                if (user == null) {
                    throw new CachingException("Cannot find user: " + cuid);
                }
                if (user.belongsToGroup(name))  // in case added elsewhere
                    throw new ServiceException(HTTP_409_CONFLICT, "User " + cuid + " already belongs to workgroup " + name);

                userServices.addUserToWorkgroup(cuid, name);
            }
            else {
                String msg = "Unsupported relationship for workgroup " + name + ": " + rel;
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
    @Path("/{groupName}")
    @ApiOperation(value="Update a workgroup", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Workgroup", paramType="body", required=true, dataType="com.centurylink.mdw.model.value.user.UserGroupVO")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {

        UserServices userServices = ServiceLocator.getUserServices();
        UserGroupVO workgroup = new UserGroupVO(content);
        String name = getSegment(path, 1);
        if (name == null)
            throw new ServiceException(HTTP_400_BAD_REQUEST, "Missing path segment: {name}");
        try {
            UserGroupVO existing = userServices.getWorkgroup(name);
            if (existing == null)
                throw new ServiceException(HTTP_404_NOT_FOUND, "Workgroup not found: " + name);
            // update
            workgroup.setId(existing.getId());
            userServices.updateWorkgroup(workgroup);
            return null;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(HTTP_500_INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    /**
     * Delete workgroup or remove user from group.
     */
    @Path("/{groupName}/users/{cuid}")
    @ApiOperation(value="Delete a workgroup or remove a user from a workgroup",
        notes="If users/{cuid} is present, user is removed from workgroup.", response=StatusMessage.class)
    public JSONObject delete(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        String name = getSegment(path, 1);
        String rel = getSegment(path, 2);

        UserServices userServices = ServiceLocator.getUserServices();
        try {
            if (rel == null) {
              userServices.deleteWorkgroup(name);
            }
            else if (rel.equals("users")) {
                String cuid = getSegment(path, 3);
                userServices.removeUserFromWorkgroup(cuid, name);
            }
        }
        catch (DataAccessException ex) {
            throw new ServiceException(HTTP_500_INTERNAL_ERROR, ex.getMessage(), ex);
        }
        return null;
    }
}
