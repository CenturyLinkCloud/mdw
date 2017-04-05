/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.event;

import java.util.Map;

import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.model.request.Request;

public interface EventHandler {
    public Response handleEventMessage(Request msg, Object msgobj, Map<String,String> metainfo)
    throws EventHandlerException;
}
