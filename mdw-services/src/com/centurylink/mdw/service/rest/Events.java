package com.centurylink.mdw.service.rest;

import java.util.Map;

import javax.ws.rs.Path;

import io.swagger.annotations.Api;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.event.Event;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.rest.JsonXmlRestService;

@Path("/Events")
@Api("Event notifications")
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
