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
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.JsonArray;
import com.centurylink.mdw.model.JsonExportable;
import com.centurylink.mdw.model.JsonListMap;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.workflow.ActivityAggregate;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.ActivityList;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.rest.JsonRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.text.ParseException;
import java.util.*;

@Path("/Activities")
@Api("Runtime activity")
public class Activities extends JsonRestService implements JsonExportable {

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Activity;
    }

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.PROCESS_EXECUTION);
        return roles;
    }

    private WorkflowServices getWorkflowServices() {
        return ServiceLocator.getWorkflowServices();
    }

    /**
     * Retrieve activity instance(s).
     */
    @Override
    @Path("/{instanceId}")
    @ApiOperation(value = "Retrieve an activity, query many activity instances, or perform special queries",
            notes = "If instanceId and special are not present, returns a page of activities that meet query criteria.",
            response = ActivityInstance.class, responseContainer = "List")
    public JSONObject get(String path, Map<String, String> headers)
            throws ServiceException, JSONException {
        try {
            String segOne = getSegment(path, 1);
            if (segOne != null) {
                try {
                    long instanceId = Long.parseLong(segOne);
                    return getWorkflowServices().getActivity(instanceId).getJson();
                }
                catch (NumberFormatException ex) {
                    // path must be special
                    Query query = getQuery(path, headers);
                    if (segOne.equals("definitions")) {
                        return getDefinitions(query);
                    }
                    else if (segOne.equals("tops")) {
                        return getTops(query).getJson();
                    }
                    else if (segOne.equals("breakdown")) {
                        return getBreakdown(query).getJson();
                    }
                    else {
                        throw new ServiceException(ServiceException.BAD_REQUEST, "Unsupported path segment: " + segOne);
                    }
                }
            }
            else {
                Query query = getQuery(path, headers);
                return getWorkflowServices().getActivities(query).getJson();
            }
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * Take {action} on activity which has {activityInstanceId}.
     * {completionCode} can be provided if none provided then it is treated as null.
     * Post http://localhost:8080/mdw/Services/Activities/40337/Proceed/sequential
     * Payload "{}"
     */
    @Override
    @Path("/{instanceId}/{action}/{completionCode}")
    @ApiOperation(value = "Take action on activity whose activity Instance ID is activityInstanceId, " +
            "if completionCode is defined then take completionCode path", response = StatusMessage.class)
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        Map<String,String> parameters = getParameters(headers);
        String instId = getSegment(path, 1);
        if (instId == null)
            instId = parameters.get("activityInstanceId");
        if (instId == null)
            throw new ServiceException("Missing parameter: activityInstanceId");
        try {
            Long activityInstanceId = Long.parseLong(instId);
            String action = getSegment(path, 2);
            if (action == null)
                action = parameters.get("action");
            if (action == null)
                throw new ServiceException("Missing parameter: action");
            String completionCode = getSegment(path, 3);
            if (completionCode == null)
                completionCode = parameters.get("completionCode");

            actionActivity(activityInstanceId, action,completionCode, getAuthUser(headers));
            return null;
        }
        catch (NumberFormatException ex) {
            throw new ServiceException(ServiceException.BAD_REQUEST, "Bad path: " + path);
        }

    }

    public Jsonable toJsonable(Query query, JSONObject json) throws JSONException {
        try {
            if (json.has(ActivityList.ACTIVITY_INSTANCES))
                return new ActivityList(ActivityList.ACTIVITY_INSTANCES, json);
            else if ("Activities/breakdown".equals(query.getPath()))
                return new JsonListMap<>(json, ActivityAggregate.class);
            else
                throw new JSONException("Unsupported export type for query: " + query);
        }
        catch (ParseException ex) {
            throw new JSONException(ex);
        }
    }

    @Override
    protected Long getEntityId(String path, Object content, Map<String,String> headers) {
        String activityInstanceId = getSegment(path, 1);
        Long id;
        try {
            id = new Long(activityInstanceId);
        }
        catch (NumberFormatException e) {
            id = 0L;
        }
        return id;
    }

    @Override
    protected Action getAction(String path, Object content, Map<String,String> headers) {
        String action = getSegment(path, 2);
        return UserAction.getAction(action);
    }

    @Path("/definitions")
    public JSONObject getDefinitions(Query query) throws ServiceException {
        ActivityList activityVOs = ServiceLocator.getDesignServices().getActivityDefinitions(query);
        JSONArray jsonActivities = new JSONArray();
        for (ActivityInstance activityInstance : activityVOs.getActivities()) {
            jsonActivities.put(activityInstance.getJson());
        }
        return activityVOs.getJson();
    }

    @Path("/tops")
    public JsonArray getTops(Query query) throws ServiceException {
        List<ActivityAggregate> list = getWorkflowServices().getTopActivities(query);
        JSONArray actArr = new JSONArray();
        int ct = 0;
        ActivityAggregate other = null;
        long otherTot = 0;
        for (ActivityAggregate actCount : list) {
            if (ct >= query.getMax()) {
                if (other == null) {
                    other = new ActivityAggregate(0);
                    other.setName("Other");
                }
                otherTot += actCount.getValue();
            } else {
                actArr.put(actCount.getJson());
            }
            ct++;
        }
        if (other != null) {
            other.setValue(otherTot);
            actArr.put(other.getJson());
        }
        return new JsonArray(actArr);
    }

    @Path("/breakdown")
    public JsonListMap<ActivityAggregate> getBreakdown(Query query) throws ServiceException {
        TreeMap<Date, List<ActivityAggregate>> dateMap = getWorkflowServices().getActivityBreakdown(query);
        LinkedHashMap<String, List<ActivityAggregate>> listMap = new LinkedHashMap<>();
        for (Date date : dateMap.keySet()) {
            List<ActivityAggregate> actCounts = dateMap.get(date);
            listMap.put(Query.getString(date), actCounts);
        }
        return new JsonListMap<>(listMap);
    }

    @Path("/{instanceId}/{action}")
    public void actionActivity(Long instanceId, String action, String result, String authUser) throws ServiceException {
        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        workflowServices.actionActivity(instanceId, action, result, authUser);
    }
}
