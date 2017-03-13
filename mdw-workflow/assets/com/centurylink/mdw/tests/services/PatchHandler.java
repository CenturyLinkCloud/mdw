/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.tests.services;

import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * Handle HTTP an PATCH request.
 */
@Path("/PatchHandler")
@Api("Test service for PATCH")
public class PatchHandler extends JsonRestService {

    @ApiOperation(value="Patch handler.", notes="Does not actually update anything", response=Workgroup.class)
        @ApiImplicitParams({
            @ApiImplicitParam(name="Workgroup", paramType="body", required=true, dataType="com.centurylink.mdw.model.user.Workgroup")
    })
    public JSONObject patch(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        // echo back the request
        return new Workgroup(content).getJson();
    }
}
