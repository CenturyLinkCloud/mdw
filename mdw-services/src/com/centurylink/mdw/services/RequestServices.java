package com.centurylink.mdw.services;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.report.Insight;
import com.centurylink.mdw.model.report.Timepoint;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.RequestAggregate;
import com.centurylink.mdw.model.request.RequestList;
import com.centurylink.mdw.model.request.ServicePath;

import java.time.Instant;
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

    List<RequestAggregate> getTopRequests(Query query) throws ServiceException;
    TreeMap<Instant,List<RequestAggregate>> getRequestBreakdown(Query query) throws ServiceException;
    List<Insight> getRequestInsights(Query query) throws ServiceException;
    List<Timepoint> getRequestTrend(Query query) throws ServiceException;

    List<String> getServicePaths(Query query) throws ServiceException;
    List<ServicePath> getRequestPaths(Query query) throws ServiceException;

    void setElapsedTime(String ownerType, Long instanceId, Long elapsedTime) throws ServiceException;
}
