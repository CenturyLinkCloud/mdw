/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * TODO: Incorporate UpdateAttributes and deprecate that class.
 */
@Path("/Attributes")
@Api("Design-time attributes")
public class Attributes extends JsonRestService {

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
     * Create attributes for owner type and id.
     */
    @Override
    @Path("/{ownerType}/{ownerId}")
    @ApiOperation(value="Create attributes for an ownerType and ownerId", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Attributes", paramType="body")})
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        return put(path, content, headers);
    }

    /**
     * Update attributes for owner type and id.
     * Existing attributes are always overwritten.
     */
    @Override
    @Path("/{ownerType}/{ownerId}")
    @ApiOperation(value="Update attributes for an ownerType and ownerId", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Attributes", paramType="body")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        Map<String,String> parameters = getParameters(headers);
        String ownerType = getSegment(path, 1);
        if (ownerType == null)
            ownerType = parameters.get("ownerType");
        if (ownerType == null)
            throw new ServiceException("Missing parameter: ownerType");
        String ownerId = getSegment(path, 2);
        if (ownerId == null)
            ownerId = parameters.get("ownerId");
        if (ownerId == null)
            throw new ServiceException("Missing parameter: ownerId");
        String updateOnly = getSegment(path, 3);
        if (updateOnly == null)
            updateOnly = parameters.get("updateOnly");
        if (content == null)
            throw new ServiceException("Missing JSON object: attributes");

        try {
            Map<String,String> attrs = new HashMap<String,String>();
            String[] names = JSONObject.getNames(content);
            if (names != null) {
                for (String name : names)
                  attrs.put(name, content.getString(name));
            }
            WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();

            if (updateOnly != null) {
                // Update attribute only, without deleting all other attributes for that ownerId
                workflowServices.updateAttributes(ownerType, Long.parseLong(ownerId), attrs);
            } else {
                workflowServices.setAttributes(ownerType, Long.parseLong(ownerId), attrs);
            }
            return null;
        }
        catch (JSONException ex) {
            throw new ServiceException(ex.getMessage(), ex);
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
