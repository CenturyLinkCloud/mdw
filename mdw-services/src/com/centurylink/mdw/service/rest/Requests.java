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
package com.centurylink.mdw.service.rest;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.*;
import com.centurylink.mdw.model.report.Insight;
import com.centurylink.mdw.model.report.Timepoint;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.RequestAggregate;
import com.centurylink.mdw.model.request.RequestList;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.RequestServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.rest.JsonRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.text.ParseException;
import java.util.*;

@Path("/Requests")
@Api("MDW master and service requests")
public class Requests extends JsonRestService implements JsonExportable {

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.PROCESS_EXECUTION);
        return roles;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Request;
    }

    private RequestServices getRequestServices() {
        return ServiceLocator.getRequestServices();
    }

    /**
     * Retrieve a request by doc id, masterRequestId, all matching requests, or an aggregated request breakdown.
     */
    @Override
    @Path("/{requestId}")
    @ApiOperation(value="Retrieve a request or a page of requests according to specified filters",
        notes="If requestId is not present, returns all matching requests.",
        response=Request.class, responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers)
    throws ServiceException, JSONException {
        RequestServices requestServices = ServiceLocator.getRequestServices();
        try {
            Query query = getQuery(path, headers);
            String segOne = getSegment(path, 1);
            if (segOne != null) {
                if (segOne.equals("tops")) {
                    return getTops(query).getJson();
                }
                else if (segOne.equals("breakdown")) {
                    return getBreakdown(query).getJson();
                }
                else if (segOne.equals("paths")) {
                    return getPaths(query).getJson();
                }
                else if (segOne.equals("insights")) {
                    JsonList<Insight> jsonList = getInsights(query);
                    JSONObject json = jsonList.getJson();
                    String trend = query.getFilter("trend");
                    if ("completionTime".equals(trend)) {
                        List<Timepoint> timepoints = requestServices.getRequestTrend(query);
                        json.put("trend", new JsonList<>(timepoints, "trend").getJson().getJSONArray("trend"));
                    }
                    return json;
                }
                else {
                    try {
                        if (query.getBooleanFilter("master")) {
                            String masterRequestId = segOne;
                            if (query.getBooleanFilter("response")) {
                                Request masterRequest = requestServices.getMasterRequestResponse(masterRequestId);
                                if (masterRequest == null)
                                    throw new ServiceException(ServiceException.NOT_FOUND, "Master request not found: " + masterRequestId);
                                return masterRequest.getJson();
                            }
                            else {
                                Request masterRequest = requestServices.getMasterRequest(masterRequestId);
                                if (masterRequest == null)
                                    throw new ServiceException(ServiceException.NOT_FOUND, "Master request not found: " + masterRequestId);
                                return masterRequest.getJson();
                            }
                        }
                        else {
                            Long requestId = Long.valueOf(segOne);
                            if (query.getBooleanFilter("request") && query.getBooleanFilter("response")) {
                                Request request = requestServices.getRequestAndResponse(requestId);
                                if (request == null)
                                    throw new ServiceException(ServiceException.NOT_FOUND, "Request not found: " + requestId);
                                return request.getJson();
                            }
                            else if (query.getBooleanFilter("response")) {
                                Request request = requestServices.getRequestResponse(requestId);
                                if (request == null)
                                    throw new ServiceException(ServiceException.NOT_FOUND, "Request not found: " + requestId);
                                return request.getJson();
                            }
                            else {
                                Request request = requestServices.getRequest(requestId);
                                if (request == null)
                                    throw new ServiceException(ServiceException.NOT_FOUND, "Request not found: " + requestId);
                                return request.getJson();
                            }
                        }
                    }
                    catch (NumberFormatException ex) {
                        throw new ServiceException(ServiceException.BAD_REQUEST, "Bad requestId: " + segOne);
                    }
                }
            }
            else {
                if (query.getLongFilter("ownerId") >= 0L) {
                    RequestList reqList = requestServices.getRequests(query);
                    if (!reqList.getItems().isEmpty())
                        return requestServices.getRequestAndResponse(reqList.getItems().get(0).getId()).getJson();
                    else
                        throw new ServiceException(ServiceException.NOT_FOUND, "Request not found for ownerId: " + query.getLongFilter("ownerId"));
                }
                else
                    return requestServices.getRequests(query).getJson();
            }
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public Jsonable toJsonable(Query query, JSONObject json) throws JSONException {
        try {
            if (json.has("requests") && RequestList.MASTER_REQUESTS.equals(query.getFilters().get("type")))
                return new RequestList(RequestList.MASTER_REQUESTS, json);
            else if (json.has("requests") && RequestList.INBOUND_REQUESTS.equals(query.getFilters().get("type")))
                return new RequestList(RequestList.INBOUND_REQUESTS, json);
            else if (json.has("requests") && RequestList.OUTBOUND_REQUESTS.equals(query.getFilters().get("type")))
                return new RequestList(RequestList.OUTBOUND_REQUESTS, json);
            else if ("Requests/breakdown".equals(query.getPath()))
                return new JsonListMap<>(json, RequestAggregate.class);
            else
                throw new JSONException("Unsupported export type for query: " + query);
        }
        catch (ParseException ex) {
            throw new JSONException(ex);
        }
    }

    @Path("/tops")
    public JsonArray getTops(Query query) throws ServiceException {
        List<RequestAggregate> list = getRequestServices().getTopRequests(query);
        JSONArray requestArray = new JSONArray();
        for (RequestAggregate requestAggregate : list) {
            requestArray.put(requestAggregate.getJson());
        }
        return new JsonArray(requestArray);
    }

    @Path("/breakdown")
    public JsonListMap<RequestAggregate> getBreakdown(Query query) throws ServiceException {
        TreeMap<Date,List<RequestAggregate>> dateMap = getRequestServices().getRequestBreakdown(query);
        LinkedHashMap<String,List<RequestAggregate>> listMap = new LinkedHashMap<>();
        for (Date date : dateMap.keySet()) {
            List<RequestAggregate> reqCounts = dateMap.get(date);
            listMap.put(Query.getString(date), reqCounts);
        }
        return new JsonListMap<>(listMap);
    }

    @Path("/insights")
    public JsonList<Insight> getInsights(Query query) throws ServiceException {
        List<Insight> requestInsights = getRequestServices().getRequestInsights(query);
        JsonList<Insight> jsonList = new JsonList<>(requestInsights, "insights");
        jsonList.setTotal(requestInsights.size());
        return jsonList;
    }

    @Path("/paths")
    public JsonArray getPaths(Query query) throws ServiceException {
        return new JsonArray(getRequestServices().getRequestPaths(query));
    }
}
