/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.event.Event;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Events")
@Api("MDW workflow event notification")
public class Events extends JsonRestService {

    @Override
    @Path("/{eventId}")
    @ApiOperation(value="Notifies of an event", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Event", paramType="body", dataType="com.centurylink.mdw.model.event.Event")})
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        String[] segments = getSegments(path);
        if (segments.length != 2)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
        String eventId = segments[1];
        Event event = new Event(content);
        if (!eventId.equals(event.getId()))
            throw new ServiceException(ServiceException.BAD_REQUEST, "Event id mismatch: " + eventId + "!=" + event.getId());

        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        workflowServices.notify(eventId, event.getMessage(), event.getDelay());
        return null;
    }
}
