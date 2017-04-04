/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.event;

import java.util.Map;

import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.model.request.Request;

public interface EventHandlerErrorResponse {
    public Response createErrorResponse(Request request, Map<String,String> metaInfo, Throwable eventHandlerException);
}
