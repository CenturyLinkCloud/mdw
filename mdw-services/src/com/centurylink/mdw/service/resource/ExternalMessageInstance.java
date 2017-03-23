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
package com.centurylink.mdw.service.resource;

import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.RequestList;
import com.centurylink.mdw.services.RequestServices;
import com.centurylink.mdw.services.ServiceLocator;

/**
 * Needed by Designer.
 */
public class ExternalMessageInstance implements JsonService {

    public String getJson(JSONObject requestJson, Map<String,String> metaInfo) throws ServiceException {
        String activityInstIdStr = (String)metaInfo.get("activityInstId");
        if (activityInstIdStr == null)
            throw new ServiceException("Missing parameter: activityInstId");
        Long activityInstId = new Long(activityInstIdStr);
        String eventInstId = (String)metaInfo.get("eventInstId");
        Long requestId = eventInstId == null ? null : new Long(eventInstId);

        try {
            JSONObject json = new JSONObject();
            RequestServices requestServices = ServiceLocator.getRequestServices();
            Query query = new Query();
            if (requestId == null) {
                // adapter/event-wait activity
                query.setFilter("type", RequestList.OUTBOUND_REQUESTS);
                query.setFilter("ownerId", activityInstId);
                RequestList reqList = requestServices.getRequests(query);
                if (!reqList.getItems().isEmpty()) {
                    Request request = reqList.getItems().get(0);
                    request = requestServices.getRequest(request.getId());
                    json.put("request", request.getContent());
                    json.put("response", requestServices.getRequestResponse(request.getId()).getResponseContent());
                }

            } else {
                // start activity
                Request request = requestServices.getRequest(requestId);
                json.put("request", request.getContent());
                json.put("response", requestServices.getRequestResponse(request.getId()).getResponseContent());
            }
            return json.toString(2);
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public String getText(Object requestObj, Map<String,String> metaInfo) throws ServiceException {
        return getJson((JSONObject)requestObj, metaInfo);
    }
}
