/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.JsonArray;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.system.SysInfoCategory;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.SystemServices;
import com.centurylink.mdw.services.SystemServices.SysInfoType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/System")
@Api("System Information")
public class System extends JsonRestService {

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
            List<SysInfoCategory> categories = systemServices.getSysInfoCategories(SysInfoType.System);
            for (SysInfoCategory category : categories)
                jsonArr.put(category.getJson());
            return new JsonArray(jsonArr).getJson();
        }
        else {
            return new JSONObject(); // TODO
        }
    }
}
