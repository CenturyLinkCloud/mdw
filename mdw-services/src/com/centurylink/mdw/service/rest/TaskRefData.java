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

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.JsonArray;
import com.centurylink.mdw.model.task.TaskCategory;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.service.data.task.TaskDataAccess;
import com.centurylink.mdw.service.data.user.UserGroupCache;
import com.centurylink.mdw.services.rest.JsonRestService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Path("/TaskRefData")
public class TaskRefData extends JsonRestService {

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
        return Entity.TaskRefData;
    }

    /**
     * Retrieve taskCategories.
     */
    @Override
    @Path("/{dataType}")
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        String dataType = getSegment(path, 1);
        if (dataType == null)
            throw new ServiceException("Missing path segment: {dataType}");
        try {
            com.centurylink.mdw.dataaccess.task.TaskRefData taskRefData = TaskDataAccess.getTaskRefData();
            if (dataType.equals("Categories")) {
                List<TaskCategory> taskCats = new ArrayList<>(taskRefData.getCategories().values());
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

