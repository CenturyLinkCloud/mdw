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
package com.centurylink.mdw.services;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.RequestCount;
import com.centurylink.mdw.model.request.RequestList;

public interface RequestServices {

    /**
     * Retrieves one page of requests.
     */
    public RequestList getRequests(Query query) throws ServiceException;

    public Request getRequest(Long id) throws ServiceException;
    public Request getRequestResponse(Long requestId) throws ServiceException;

    public Request getMasterRequest(String masterRequestId) throws ServiceException;
    public Request getMasterRequestResponse(String masterReqeustId) throws ServiceException;

    public Map<Date,List<RequestCount>> getRequestBreakdown(Query query) throws ServiceException;

}
