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
import com.centurylink.mdw.model.JsonArray;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.rest.JsonRestService;

@Path("/ValueHolders")
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
