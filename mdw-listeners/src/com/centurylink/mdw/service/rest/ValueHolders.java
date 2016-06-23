/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.JsonArray;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/ValueHolders")
@Api("Runtime value holders")
public class ValueHolders extends JsonRestService {

    @Override
    protected Entity getEntity(String path, Object content, Map<String, String> headers) {
        return Entity.ValueHolder;
    }

    /**
     * Retrieve value holder IDs for specific names/values (optionally restricted by OwnerType).
     */
    @Override
    @Path("/{valueName}/{value}")
    @ApiOperation(value = "Retrieve holder IDs for specific names and value patterns (supports *).",
            response=String.class, responseContainer="Array")
    @ApiImplicitParams({
        @ApiImplicitParam(name="holderType", paramType="query", dataType="string")})
    public JSONObject get(String path, Map<String, String> headers)
            throws ServiceException, JSONException {
        Map<String, String> parameters = getParameters(headers);
        String valueName = getSegment(path, 1);
        if (valueName == null)
            throw new ServiceException("Missing path segment: {valueName}");
        String valuePattern = getSegment(path, 2);
        String ownerType = parameters.get("holderType");

        try {
            List<String> ids = ServiceLocator.getWorkflowServices().getValueHolderIds(valueName, valuePattern, ownerType);
            return new JsonArray(ids).getJson();
        }
        catch (Exception ex) {
            throw new ServiceException("Error loading value holders for " + valueName, ex);
        }
    }

}
