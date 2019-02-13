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
package com.centurylink.mdw.services.request;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.reports.RequestAggregation;
import com.centurylink.mdw.dataaccess.reports.RequestInsights;
import com.centurylink.mdw.model.report.Insight;
import com.centurylink.mdw.model.report.Timepoint;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.RequestAggregate;
import com.centurylink.mdw.model.request.RequestList;
import com.centurylink.mdw.model.request.ServicePath;
import com.centurylink.mdw.service.data.RequestDataAccess;
import com.centurylink.mdw.service.data.ServicePaths;
import com.centurylink.mdw.services.RequestServices;
import com.centurylink.mdw.util.timer.CodeTimer;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RequestServicesImpl implements RequestServices {

    private RequestDataAccess getDAO() {
        return new RequestDataAccess();
    }

    protected RequestAggregation getRequestAggregation() {
        return new RequestAggregation();
    }

    public RequestList getRequests(Query query) throws ServiceException {
        String requestType = query.getFilters().get("type");

        if (requestType == null)
            requestType = RequestList.MASTER_REQUESTS;
        else
            query.getFilters().remove("type");  // remove from filters

        try {
            if (RequestList.MASTER_REQUESTS.equals(requestType))
                return getDAO().getMasterRequests(query);
            else if (RequestList.INBOUND_REQUESTS.equals(requestType))
                return getDAO().getInboundRequests(query);
            else if (RequestList.OUTBOUND_REQUESTS.equals(requestType))
                return getDAO().getOutboundRequests(query);
            else
                throw new ServiceException(ServiceException.BAD_REQUEST, "Unsupported request type: " + requestType);
        }
        catch (DataAccessException ex) {
            throw new ServiceException("Failed to retrieve " + requestType + " for query: " + query, ex);
        }
    }

    public Request getRequest(Long id) throws ServiceException {
        try {
           Request request = getDAO().getRequest(id, true, false);
           if (request == null)
               throw new ServiceException(ServiceException.NOT_FOUND, "Request not found: " + id);
           return request;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Failed to get requestId: " + id, ex);
        }
    }

    public Request getMasterRequest(String masterRequestId) throws ServiceException {
        try {
           return getDAO().getMasterRequest(masterRequestId, true, false);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Failed to get masterRequestId: " + masterRequestId, ex);
        }
    }

    public Request getRequestResponse(Long requestId) throws ServiceException {
        try {
            Request request = getDAO().getRequest(requestId, false, true);
            if (request == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Request not found: " + requestId);
            return request;
         }
         catch (DataAccessException ex) {
             throw new ServiceException(ServiceException.INTERNAL_ERROR, "Failed to get response for requestId: " + requestId, ex);
         }
    }

    public Request getRequestAndResponse(Long requestId) throws ServiceException {
        try {
            Request request = getDAO().getRequest(requestId, true, true);
            if (request == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Request not found: " + requestId);
            return request;
         }
         catch (DataAccessException ex) {
             throw new ServiceException(ServiceException.INTERNAL_ERROR, "Failed to get response for requestId: " + requestId, ex);
         }
    }

    public Request getMasterRequestResponse(String masterRequestId) throws ServiceException {
        try {
            Request request = getDAO().getMasterRequest(masterRequestId, false, true);
            if (request == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Master request not found: " + masterRequestId);
            return request;
         }
         catch (DataAccessException ex) {
             throw new ServiceException(ServiceException.INTERNAL_ERROR, "Failed to get response for masterRequestId: " + masterRequestId, ex);
         }
    }

    public List<RequestAggregate> getTopRequests(Query query) throws ServiceException {
        try {
            CodeTimer timer = new CodeTimer(true);
            List<RequestAggregate> list = getRequestAggregation().getTops(query);
            timer.stopAndLogTiming("RequestServicesImpl.getTopRequests()");
            return list;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving top throughput requests: query=" + query, ex);
        }
    }

    public TreeMap<Date,List<RequestAggregate>> getRequestBreakdown(Query query) throws ServiceException {
        try {
            return getRequestAggregation().getBreakdown(query);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving request breakdown: query=" + query, ex);
        }
    }

    @Override
    public void setElapsedTime(String ownerType, Long instanceId, Long elapsedTime) throws ServiceException {
        try {
            getDAO().setElapsedTime(ownerType, instanceId, elapsedTime);
        }
        catch (SQLException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    @Override
    public List<String> getServicePaths(Query query) {
        Stream<String> stream;
        if ("out".equals(query.getFilter("direction")))
            stream = ServicePaths.getOutboundPaths().stream().map(Object::toString);
        else
            stream = ServicePaths.getInboundPaths().stream().map(Object::toString);

        return stream.sorted(String::compareToIgnoreCase).collect(Collectors.toList());
    }

    @Override
    public List<ServicePath> getRequestPaths(Query query) throws ServiceException {
        try {
            return new RequestAggregation().getPaths(query);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving request paths: query=" + query, ex);
        }
    }

    @Override
    public List<Insight> getRequestInsights(Query query) throws ServiceException {
        try {
            return new RequestInsights().getInsights(query);
        }
        catch (SQLException | ParseException ex) {
            throw new ServiceException(500, "Error retrieving request insights: query=" + query, ex);
        }
    }

    @Override
    public List<Timepoint> getRequestTrend(Query query) throws ServiceException {
        try {
            return new RequestInsights().getTrend(query);
        }
        catch (SQLException | ParseException ex) {
            throw new ServiceException(500, "Error retrieving request trend: query=" + query, ex);
        }
    }
}
