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

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.JsonExportable;
import com.centurylink.mdw.model.JsonListMap;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.RequestCount;
import com.centurylink.mdw.model.request.RequestList;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.RequestServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

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
                if (segOne.equals("instanceCounts")) {
                    Map<Date,List<RequestCount>> dateMap = requestServices.getRequestBreakdown(query);
                    Map<String,List<RequestCount>> listMap = new HashMap<String,List<RequestCount>>();
                    for (Date date : dateMap.keySet()) {
                        List<RequestCount> reqCounts = dateMap.get(date);
                        listMap.put(Query.getString(date), reqCounts);
                    }

                    return new JsonListMap<RequestCount>(listMap).getJson();
                }
                else {
                    try {
                        if (query.getBooleanFilter("master")) {
                            String masterRequestId = segOne;
                            if (query.getBooleanFilter("response"))
                                return requestServices.getMasterRequestResponse(masterRequestId).getJson();
                            else
                                return requestServices.getMasterRequest(masterRequestId).getJson();
                        }
                        else {
                            Long requestId = Long.valueOf(segOne);
                            if (query.getBooleanFilter("request") && query.getBooleanFilter("response"))
                                return requestServices.getRequestAndResponse(requestId).getJson();
                            else if (query.getBooleanFilter("response"))
                                return requestServices.getRequestResponse(requestId).getJson();
                            else
                                return requestServices.getRequest(requestId).getJson();
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
