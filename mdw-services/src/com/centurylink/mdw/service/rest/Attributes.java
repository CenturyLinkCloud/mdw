/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.JsonUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Attributes")
@Api("Design-time attributes")
public class Attributes extends JsonRestService {

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.ANY); // TODO: for now this is needed for Designer access
        return roles;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Attribute;
    }

    /**
     * Retrieve attributes.
     */
    @Override
    @Path("/{ownerType}/{ownerId}")
    @ApiOperation(value="Retrieve attributes for an ownerType and ownerId",
        notes="Response is a generic JSON object with names/values.")
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        Map<String,String> parameters = getParameters(headers);
        String ownerType = getSegment(path, 1);
        if (ownerType == null) // fall back to parameter
            ownerType = parameters.get("ownerType");
        if (ownerType == null)
            throw new ServiceException("Missing path segment: {ownerType}");
        String ownerId = getSegment(path, 2);
        if (ownerId == null) // fall back to parameter
            ownerId = parameters.get("ownerId");
        if (ownerId == null)
            throw new ServiceException("Missing path segment: {ownerId}");

        try {
            Map<String,String> attrs = ServiceLocator.getWorkflowServices().getAttributes(ownerType, Long.parseLong(ownerId));
            JSONObject attrsJson = new JSONObject();
            for (String name : attrs.keySet())
                attrsJson.put(name, attrs.get(name));
            return attrsJson;
        }
        catch (Exception ex) {
            throw new ServiceException("Error loading attributes for " + ownerType + ": " + ownerId, ex);
        }
    }

    /**
     * Update attributes owner type and id (does not delete existing ones).
     */
    @Override
    @Path("/{ownerType}/{ownerId}")
    @ApiOperation(value="Update attributes for an ownerType and ownerId", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Attributes", paramType="body", required=true)})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        String ownerType = getSegment(path, 1);
        if (ownerType == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing pathSegment: ownerType");
        String ownerId = getSegment(path, 2);
        if (ownerId == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing pathSegment: ownerId");

        try {
            Map<String,String> attrs = JsonUtil.getMap(content);
            ServiceLocator.getWorkflowServices().updateAttributes(ownerType, Long.parseLong(ownerId), attrs);
            return null;
        }
        catch (NumberFormatException ex) {
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid ownerId: " + ownerId);
        }
        catch (JSONException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    /**
     * Set attributes for owner type and id.
     * Existing attributes are always overwritten.
     */
    @Override
    @Path("/{ownerType}/{ownerId}")
    @ApiOperation(value="Set attributes for an ownerType and ownerId", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Attributes", paramType="body", required=true)})
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        String ownerType = getSegment(path, 1);
        if (ownerType == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing pathSegment: ownerType");
        String ownerId = getSegment(path, 2);
        if (ownerId == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing pathSegment: ownerId");

        try {
            Map<String,String> attrs = JsonUtil.getMap(content);
            ServiceLocator.getWorkflowServices().setAttributes(ownerType, Long.parseLong(ownerId), attrs);
            return null;
        }
        catch (NumberFormatException ex) {
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid ownerId: " + ownerId);
        }
        catch (JSONException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    /**
     * Delete attributes for owner type and id.
     */
    @Override
    @Path("/{ownerType}/{ownerId}")
    @ApiOperation(value="Delete attributes for an ownerType and ownerId", response=StatusMessage.class)
    public JSONObject delete(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        JSONObject empty = new JSONObject();
        return put(path, empty, headers);
    }
}
