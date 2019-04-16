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

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.JsonArray;
import com.centurylink.mdw.model.report.MetricDataList;
import com.centurylink.mdw.model.system.SysInfoCategory;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.SystemServices;
import com.centurylink.mdw.services.SystemServices.SysInfoType;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.services.system.SystemMetrics;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Path("/System")
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
        else if (segments.length >= 3 && segments[1].equals("metrics")) {
            String metric = segments[2];
            Query query = getQuery(path, headers);
            int span = query.getIntFilter("span");
            if (span == -1)
                span = 300; // 5 minutes
            SystemMetrics systemMetrics = SystemMetrics.getInstance();
            MetricDataList data = systemMetrics.getData(metric);
            if (segments.length == 4 && segments[3].equals("summary")) {
                return new JsonArray(data.getAverages(span)).getJson();
            }
            else if (segments.length == 3) {
                return data.getJson(span);
            }
        }
        throw new ServiceException(ServiceException.BAD_REQUEST, "Unsupported path: " + path);
    }
}
