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
        String rcHeader =  headers.get("send-response-code");
        if (rcHeader != null)
            throw new ServiceException(Integer.parseInt(rcHeader), "Sending response code " + rcHeader);
        // echo back the request
        return new Workgroup(content).getJson();
    }
}
