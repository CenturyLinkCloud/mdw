/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Exportable;
import com.centurylink.mdw.common.service.JsonArray;
import com.centurylink.mdw.common.service.JsonListMap;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.value.requests.Request;
import com.centurylink.mdw.model.value.requests.RequestCount;
import com.centurylink.mdw.model.value.requests.RequestList;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.services.RequestServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/Requests")
@Api("MDW master and service requests")
public class Requests extends JsonRestService implements Exportable {

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(UserRoleVO.PROCESS_EXECUTION);
        return roles;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Request;
    }

    /**
     * Retrieve a request by id, all matching requests, or an aggregated request breakdown.
     */
    @Override
    @Path("/{requestId}")
    @ApiOperation(value="Retrieve a request or a page of requests according to specified filters",
        notes="If requestId is not present, returns all matching requests.",
        response=Request.class, responseContainer="List")
    public JSONObject get(String path, Map<String, String> headers)
            throws ServiceException, JSONException {
        RequestServices requestServices = ServiceLocator.getRequestServices();
        try {
            String segOne = getSegment(path, 1);
            if (segOne != null) {
                try {
                    return requestServices.getRequest(segOne).getJson();
                }
                catch (NumberFormatException ex) {
                    // path must be special
                    Query query = getQuery(path, headers);
                    if (segOne.equals("definitions")) {
                        RequestList requestList = requestServices.getRequests(query);
                        JSONArray jsonRequests = new JSONArray();
                        for (Request request : requestList.getRequests()) {
                            JSONObject jsonRequest = new JSONObject();
                            jsonRequest.put("packageName", request.getPackageName());
                            jsonRequest.put("processId", request.getId());
                            jsonRequest.put("name", request.getProcessName());                // TODO:???
                            jsonRequest.put("version", request.getProcessVersion());
                            jsonRequests.put(jsonRequest);
                        }
                        return new JsonArray(jsonRequests).getJson();
                    }
                    // TODO: ???
//                    else if (segOne.equals("topThroughput")) {
//                        List<RequestCount> list = requestServices.getTopThroughputProcesses(query);
//                        JSONArray procArr = new JSONArray();
//                        int ct = 0;
//                        RequestCount other = null;
//                        long otherTot = 0;
//                        for (RequestCount requestCount : list) {
//                            if (ct >= query.getMax()) {
//                                if (other == null) {
//                                    other = new RequestCount(0);
//                                    other.setType("Other");
//                                }
//                                otherTot += requestCount.getCount();
//                            }
//                            else {
//                                procArr.put(requestCount.getJson());
//                            }
//                            ct++;
//                        }
//                        if (other != null) {
//                            other.setCount(otherTot);
//                            procArr.put(other.getJson());
//                        }
//                        return new JsonArray(procArr).getJson();
//                    }
                    else if (segOne.equals("instanceCounts")) {
                        Map<Date, List<RequestCount>> dateMap = requestServices.getRequestBreakdown(query); // TODO: ???
                        boolean isTotals = query.getFilters().get("requestIds") == null && query.getFilters().get("statuses") == null;
                        Map<String, List<RequestCount>> listMap = new HashMap<String, List<RequestCount>>();

                        for (Date date : dateMap.keySet()) {
                            List<RequestCount> requestCounts = dateMap.get(date);
                            if (isTotals) {
                                for (RequestCount requestCount : requestCounts)
                                    requestCount.setType("Total");                 // TODO: ???
                            }
                            listMap.put(Query.getString(date), requestCounts);
                        }
                        return new JsonListMap<RequestCount>(listMap).getJson();
                    }
                    else {
                        throw new ServiceException(ServiceException.BAD_REQUEST, "Unsupported path segment: " + segOne);
                    }
                }
            }
            else {
                Query query = getQuery(path, headers);
                return requestServices.getRequests(query).getJson();
            }
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

//    public JSONObject get(String path, Map<String,String> headers)
//    throws ServiceException, JSONException {
//        RequestServices requestServices = ServiceLocator.getRequestServices();
//        try {
//            Query query = getQuery(path, headers);
//            String segOne = getSegment(path, 1);
//            if (segOne != null) {
//                if (segOne.equals("instanceCounts")) {
//                    Map<Date,List<RequestCount>> dateMap = requestServices.getRequestBreakdown(query);
//                    Map<String,List<RequestCount>> listMap = new HashMap<String,List<RequestCount>>();
//                    for (Date date : dateMap.keySet()) {
//                        List<RequestCount> reqCounts = dateMap.get(date);
//                        listMap.put(Query.getString(date), reqCounts);
//                    }
//
//                    return new JsonListMap<RequestCount>(listMap).getJson();
//                }
//                else {
//                    String requestId = segOne;   //TODO: The segOne is already String.
//                    return requestServices.getRequest(requestId).getJson();
//                }
//            }
//            else {
//                return requestServices.getRequests(query).getJson();  //First time, it comes here to fetch the list to be displayed. Also for the filter call.
//            }
//        }
//        catch (ServiceException ex) {
//            throw ex;
//        }
//        catch (Exception ex) {
//            throw new ServiceException(ex.getMessage(), ex);
//        }
//    }

    public Jsonable toJsonable(Query query, JSONObject json) throws JSONException {
        try {
            if (json.has("requests") && RequestList.MASTER_REQUESTS.equals(query.getFilters().get("type")))
                return new RequestList(RequestList.MASTER_REQUESTS, json);
            else if (json.has("requests") && RequestList.INBOUND_REQUESTS.equals(query.getFilters().get("type")))
                return new RequestList(RequestList.INBOUND_REQUESTS, json);
            else if (json.has("requests") && RequestList.OUTBOUND_REQUESTS.equals(query.getFilters().get("type")))
                return new RequestList(RequestList.OUTBOUND_REQUESTS, json);
            else if ("Requests/instanceCounts".equals(query.getPath()))
                return new JsonListMap<RequestCount>(json, RequestCount.class);
            else
                throw new JSONException("Unsupported export type for query: " + query);
        }
        catch (ParseException ex) {
            throw new JSONException(ex);
        }

    }

}
