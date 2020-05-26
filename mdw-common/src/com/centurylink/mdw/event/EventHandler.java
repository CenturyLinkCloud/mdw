/*
 * Copyright (C) 2020 CenturyLink, Inc.
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
package com.centurylink.mdw.event;

import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.Response;
import com.centurylink.mdw.request.RequestHandler;
import com.centurylink.mdw.request.RequestHandlerException;

import java.util.Map;

/**
 * @deprecated
 * use {@link com.centurylink.mdw.request.RequestHandlerException}
 */
@Deprecated
public interface EventHandler extends RequestHandler {
    Response handleEventMessage(Request request, Object message, Map<String,String> headers)
    throws EventHandlerException;

    default Response handleRequest(Request request, Object message, Map<String,String> headers)
            throws RequestHandlerException {
        return handleEventMessage(request, message, headers);
    }
}
