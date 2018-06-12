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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.BaselineData;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.model.JsonArray;
import com.centurylink.mdw.model.task.TaskCategory;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.model.variable.VariableType;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.services.rest.JsonRestService;

@Path("/BaseData")
public class BaseData extends JsonRestService {

    @Override
    public List<String> getRoles(String path, String method) {
        if (method.equals("GET")) {
            List<String> roles = new ArrayList<>();
            if (UserGroupCache.getRole(Role.ASSET_VIEW) != null) {
                roles.add(Role.ASSET_VIEW);
                roles.add(Role.ASSET_DESIGN);
                roles.add(Workgroup.SITE_ADMIN_GROUP);
            }
            return roles;
        }
        else {
            return super.getRoles(path);
        }
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.BaseData;
    }

    /**
     * Retrieve variableTypes or taskCategories.
     */
    @Override
    @Path("/{dataType}")
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        String dataType = getSegment(path, 1);
        if (dataType == null)
            throw new ServiceException("Missing path segment: {dataType}");
        try {
            BaselineData baselineData = DataAccess.getBaselineData();
            if (dataType.equals("VariableTypes")) {
                List<VariableType> variableTypes = baselineData.getVariableTypes();
                JSONArray jsonArray = new JSONArray();
                for (VariableType variableType : variableTypes)
                    jsonArray.put(variableType.getJson());
                return new JsonArray(jsonArray).getJson();
            }
            else if (dataType.equals("TaskCategories")) {
                List<TaskCategory> taskCats = new ArrayList<TaskCategory>();
                taskCats.addAll(baselineData.getTaskCategories().values());
                Collections.sort(taskCats);
                JSONArray jsonArray = new JSONArray();
                for (TaskCategory taskCat : taskCats)
                    jsonArray.put(taskCat.getJson());
                return new JsonArray(jsonArray).getJson();
            }
            else {
                throw new ServiceException("Unsupported dataType: " + dataType);
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }
}

