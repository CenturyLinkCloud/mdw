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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.model.JsonArray;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.system.SysInfoCategory;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.SystemServices;
import com.centurylink.mdw.services.SystemServices.SysInfoType;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.log.LoggerUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/System")
@Api("System Information & Configuration")
public class System extends JsonRestService {

    /**
     * ASSET_DESIGN role can PUT in-memory config for running tests.
     */
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.ASSET_DESIGN);
        return roles;
    }

    @Override
    @Path("/{sysInfoType}/{category}")
    @ApiOperation(value="Retrieve system information",
        notes="The sysInfoType path segment is mandatory.  If {category} is not present, "
                + "returns a shallow list of available sysInfoCategories.",
        response=SysInfoCategory.class, responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        SystemServices systemServices = ServiceLocator.getSystemServices();
        String[] segments = getSegments(path);
        if (segments.length == 2) {
            JSONArray jsonArr = new JSONArray();
            try {
                SysInfoType type = segments[1].equals("sysInfo") ? SysInfoType.System : SysInfoType.valueOf(segments[1]);
                List<SysInfoCategory> categories = systemServices.getSysInfoCategories(type, getQuery(path, headers));
                for (SysInfoCategory category : categories)
                    jsonArr.put(category.getJson());
                return new JsonArray(jsonArr).getJson();
            }
            catch (IllegalArgumentException ex) {
                throw new ServiceException(ServiceException.BAD_REQUEST, "Unsupported SysInfoType: " + segments[1]);
            }
        }
        else {
            return new JsonObject(); // TODO
        }
    }

    @Override
    @Path("/config")
    @ApiOperation(value="Update configuration", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="ConfigValues", paramType="body", required=true, dataType="java.lang.Object")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        String[] segments = getSegments(path);
        if (segments.length != 2 || !"config".equals(segments[1]))
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid request path: " + path);

        PropertyManager propMgr = PropertyManager.getInstance();
        for (String name : JSONObject.getNames(content)) {
            Object v = content.get(name);
            String value = v == null ? null : v.toString();
            propMgr.setStringProperty(name, value == null || value.isEmpty() ? null : value);
        }
        LoggerUtil.getStandardLogger().refreshCache();

        try {
            propagatePut(content, headers);
        }
        catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }

        return null;
    }
}
