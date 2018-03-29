/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.Workgroup;

public interface EventHandler {
    public Response handleEventMessage(Request msg, Object msgobj, Map<String,String> metainfo)
    throws EventHandlerException;

    public default List<String> getRoles() {
        List<String> defaultRoles = new ArrayList<String>();
        defaultRoles.add(Workgroup.SITE_ADMIN_GROUP);
        defaultRoles.add(Role.PROCESS_EXECUTION);
        return defaultRoles;
    }
}
