package com.centurylink.mdw.request;

import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.Response;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.Workgroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface RequestHandler {

    enum Routing {
        Content,
        Path
    }

    /**
     * Handles an incoming request
     * @param request request from external system in string format
     * @param message message from external system in parsed form, such as XMLObject or JSONObject.
     * @param headers meta information around the request message, such as protocol,
     *     reply to address, request id, event name, event id, event instance id,
     *     arguments specified for handler, etc. See constants in Listener.java
     *     for typical ones
     */
    Response handleRequest(Request request, Object message, Map<String,String> headers)
            throws RequestHandlerException;

    default List<String> getRoles() {
        List<String> defaultRoles = new ArrayList<>();
        defaultRoles.add(Workgroup.SITE_ADMIN_GROUP);
        defaultRoles.add(Role.PROCESS_EXECUTION);
        return defaultRoles;
    }
}
