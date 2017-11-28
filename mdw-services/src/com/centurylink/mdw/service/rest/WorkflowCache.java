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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.startup.StartupException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/WorkflowCache")
@Api("Workflow cache refresh")
public class WorkflowCache extends JsonRestService {

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.PROCESS_DESIGN);
        return roles;
    }

    @Override
    protected Action getAction(String path, Object content, Map<String, String> headers) {
        if ("POST".equals(headers.get(Listener.METAINFO_HTTP_METHOD)))
            return Action.Refresh;
        else
            return super.getAction(path, content, headers);
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String, String> headers) {
        return Entity.Cache;
    }

    @Override
    @Path("/{cacheName}")
    @ApiOperation(value="Refresh the entire MDW workflow cache, or an individual cache",
        notes="If {cacheName} is present a single cache is refresh, otherwise all caches are refreshed", response=StatusMessage.class)
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {

        try {
            String singleCacheName = getSegment(path, 1);

            if (singleCacheName == null) {
                List<String> excludeFormats = null;
                if (content.has("excludeFormats")) {
                    excludeFormats = new ArrayList<>();
                    JSONArray jsonArr = content.getJSONArray("excludeFormats");
                    for (int i = 0; i < jsonArr.length(); i++) {
                        excludeFormats.add(jsonArr.getString(i));
                    }
                }
                CacheRegistration.getInstance().refreshCaches(excludeFormats);
            }
            else {
                new CacheRegistration().refreshCache(singleCacheName);
            }

            if (content.has("distributed") && content.getBoolean("distributed"))
                propagatePost(content, headers);
        }
        catch (StartupException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
        }
        catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
        }

        return null;
    }
}
