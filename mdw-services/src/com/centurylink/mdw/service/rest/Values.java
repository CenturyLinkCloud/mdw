/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.HashMap;
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
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Values")
@Api("Runtime values")
public class Values extends JsonRestService {

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.PROCESS_EXECUTION);
        return roles;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Value;
    }

    /**
     * Retrieve values.
     */
    @Override
    @Path("/{ownerType}/{ownerId}")
    @ApiOperation(value="Retrieve values for an ownerType and ownerId",
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

        JSONObject valuesJson = new JSONObject();
        Map<String,String> values = ServiceLocator.getWorkflowServices().getValues(ownerType, ownerId);
        if (values != null) {
            for (String name : values.keySet())
                valuesJson.put(name, values.get(name));
        }
        return valuesJson;
    }

    /**
     * Create values for owner type and id.
     */
    @Override
    @Path("/{ownerType}/{ownerId}")
    @ApiOperation(value="Create values for an ownerType and ownerId", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Values", paramType="body")})
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        return put(path, content, headers);
    }

    /**
     * Update values for owner type and id.
     * Existing values are always overwritten.
     */
    @Override
    @Path("/{ownerType}/{ownerId}")
    @ApiOperation(value="Update values for an ownerType and ownerId", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Values", paramType="body")})
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
            Map<String,String> values = new HashMap<String,String>();
            String[] names = JSONObject.getNames(content);
            if (names != null) {
                for (String name : names)
                    values.put(name, content.getString(name));
            }
            WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();

            if (updateOnly != null) {
                // update values, without deleting all other values for that ownerId
                workflowServices.updateValues(ownerType, ownerId, values);
            } else {
                workflowServices.setValues(ownerType, ownerId, values);
            }
            return null;
        }
        catch (JSONException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * Delete values for owner type and id.
     */
    @Override
    @Path("/{ownerType}/{ownerId}")
    @ApiOperation(value="Delete values for an ownerType and ownerId", response=StatusMessage.class)
    public JSONObject delete(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        JSONObject empty = new JSONObject();
        return put(path, empty, headers);
    }
}
