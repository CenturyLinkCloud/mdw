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

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.RequestAggregate;
import com.centurylink.mdw.model.request.RequestList;

import java.util.Date;
import java.util.List;
import java.util.TreeMap;

public interface RequestServices {

    /**
     * Retrieves one page of requests.
     */
    RequestList getRequests(Query query) throws ServiceException;

    Request getRequest(Long id) throws ServiceException;
    Request getRequestResponse(Long requestId) throws ServiceException;
    Request getRequestAndResponse(Long requestId) throws ServiceException;

    Request getMasterRequest(String masterRequestId) throws ServiceException;
    Request getMasterRequestResponse(String masterReqeustId) throws ServiceException;

    TreeMap<Date,List<RequestAggregate>> getRequestBreakdown(Query query) throws ServiceException;

    void setElapsedTime(String ownerType, Long instanceId, Long elapsedTime) throws ServiceException;
}
