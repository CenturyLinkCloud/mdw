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

import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.event.Event;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.rest.JsonXmlRestService;

@Path("/Events")
public class Events extends JsonXmlRestService {

    @Override
    @Path("/{eventId}")
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        String[] segments = getSegments(path);
        if (segments.length != 2)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
        String eventId = segments[1];
        Event event = new Event(content);
        if (!eventId.equals(event.getId()))
            throw new ServiceException(ServiceException.BAD_REQUEST, "Event id mismatch: " + eventId + "!=" + event.getId());

        if (event.getMessage() == null)
            event.setMessage("Empty");

        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        workflowServices.notify(eventId, event.getMessage(), event.getDelay());
        return null;
    }
}
