/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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
