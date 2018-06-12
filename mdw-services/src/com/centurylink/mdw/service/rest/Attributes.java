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

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.JsonUtil;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Attributes")
public class Attributes extends JsonRestService {

    @Override
    public List<String> getRoles(String path) {
        return null; // TODO: for now this is needed for Designer access
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
            JSONObject attrsJson = new JsonObject();
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
        @ApiImplicitParam(name="Attributes", paramType="body", required=true, dataType="java.lang.Object")})
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
        @ApiImplicitParam(name="Attributes", paramType="body", required=true, dataType="java.lang.Object")})
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
        JSONObject empty = new JsonObject();
        return post(path, empty, headers);
    }
}
