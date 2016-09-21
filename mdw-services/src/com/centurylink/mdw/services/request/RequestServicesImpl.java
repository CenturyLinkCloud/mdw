/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.request;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.file.AggregateDataAccessVcs;
import com.centurylink.mdw.model.value.requests.Request;
import com.centurylink.mdw.model.value.requests.RequestCount;
import com.centurylink.mdw.model.value.requests.RequestList;
import com.centurylink.mdw.services.RequestServices;
import com.centurylink.mdw.services.dao.RequestsDAO;

public class RequestServicesImpl implements RequestServices {

    private RequestsDAO getDAO() {
        return new RequestsDAO();
    }

    protected AggregateDataAccessVcs getAggregateDataAccess() throws DataAccessException {
        return new AggregateDataAccessVcs();
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
           return getDAO().getRequest(id, true, false);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Failed to get requestId: " + id, ex);
        }
    }

    public Request getRequestResponse(Long requestId) throws ServiceException {
        try {
            return getDAO().getRequest(requestId, false, true);
         }
         catch (DataAccessException ex) {
             throw new ServiceException(ServiceException.INTERNAL_ERROR, "Failed to get response for requestId: " + requestId, ex);
         }
    }

    public Map<Date,List<RequestCount>> getRequestBreakdown(Query query) throws ServiceException {
        try {
            Map<Date,List<RequestCount>> map = getAggregateDataAccess().getRequestBreakdown(query);
            return map;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving request breakdown: query=" + query, ex);
        }
    }

}
