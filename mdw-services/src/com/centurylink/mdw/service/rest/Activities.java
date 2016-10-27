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

import com.centurylink.mdw.common.service.JsonExportable;
import com.centurylink.mdw.common.service.JsonArray;
import com.centurylink.mdw.common.service.JsonListMap;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.workflow.ActivityCount;
import com.centurylink.mdw.model.workflow.ActivityInstanceInfo;
import com.centurylink.mdw.model.workflow.ActivityList;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

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

    /**
     * Retrieve activity instance(s).
     */
    @Override
    @Path("/{instanceId|special}")
    @ApiOperation(value="Retrieve an activity, query many activity instances, or perform special queries",
        notes="If instanceId and special are not present, returns a page of activities that meet query criteria.",
        response=ActivityInstanceInfo.class, responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers)
    throws ServiceException, JSONException {
        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        try {
            String segOne = getSegment(path, 1);
            if (segOne != null) {
              try {
                long instanceId = Long.parseLong(segOne);
                return workflowServices.getActivity(instanceId).getJson();
            }
              catch (NumberFormatException ex) {
             // path must be special
                Query query = getQuery(path, headers);
                if (segOne.equals("definitions")) {
                    ActivityList activityVOs = workflowServices.getActivityDefinitions(query);
                    JSONArray jsonActivities = new JSONArray();
                    for (ActivityInstanceInfo activityInstance : activityVOs.getActivities()) {
                        jsonActivities.put(activityInstance.getJson());
                    }
                    //return new JsonArray(jsonActivities).getJson();
                    return activityVOs.getJson();
                }

              else if (segOne.equals("topThroughput")) {
                List<ActivityCount> list = workflowServices.getTopThroughputActivities(query);
                JSONArray actArr = new JSONArray();
                int ct = 0;
                ActivityCount other = null;
                long otherTot = 0;
                for (ActivityCount actCount : list) {
                    if (ct >= query.getMax()) {
                        if (other == null) {
                            other = new ActivityCount(0);
                            other.setName("Other");
                        }
                        otherTot += actCount.getCount();
                    }
                    else {
                        actArr.put(actCount.getJson());
                    }
                    ct++;
                }
                if (other != null) {
                    other.setCount(otherTot);
                    actArr.put(other.getJson());
                }
                return new JsonArray(actArr).getJson();

              }
                else if (segOne.equals("instanceCounts")) {
                    Map<Date,List<ActivityCount>> dateMap = workflowServices.getActivityInstanceBreakdown(query);
                    boolean isTotals = query.getFilters().get("activityIds") == null && query.getFilters().get("statuses") == null;

                    Map<String,List<ActivityCount>> listMap = new HashMap<String,List<ActivityCount>>();
                    for (Date date : dateMap.keySet()) {
                        List<ActivityCount> actCounts = dateMap.get(date);
                        if (isTotals) {
                            for (ActivityCount actCount : actCounts)
                                actCount.setName("Total");
                        }
                        listMap.put(Query.getString(date), actCounts);
                    }
                    return new JsonListMap<ActivityCount>(listMap).getJson();
                }
                else {
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Unsupported path segment: " + segOne);
                }
              }
            }
            else {
                Query query = getQuery(path, headers);
                return workflowServices.getActivities(query).getJson();
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
    @Path("/{activityInstanceId}/{action}/{completionCode}")
    @ApiOperation(value="Take action on activity whose activity Instance ID is activityInstanceId, if completionCode is defined then take completionCode path", response=StatusMessage.class)
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        Map<String,String> parameters = getParameters(headers);
        String activityInstanceId = getSegment(path, 1);
        if (activityInstanceId == null)
            activityInstanceId = parameters.get("activityInstanceId");
        if (activityInstanceId == null)
            throw new ServiceException("Missing parameter: activityInstanceId");
        String action = getSegment(path, 2);
        if (action == null)
            action = parameters.get("action");
        if (action == null)
            throw new ServiceException("Missing parameter: action");
        String completionCode = getSegment(path, 3);
        if (completionCode == null)
            completionCode = parameters.get("completionCode");
        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        workflowServices.actionActivity(activityInstanceId, action, completionCode);
        return null;

    }

    public Jsonable toJsonable(Query query, JSONObject json) throws JSONException {
        try {
            if (json.has(ActivityList.ACTIVITY_INSTANCES))
                return new ActivityList(ActivityList.ACTIVITY_INSTANCES, json);
            else if ("Activities/instanceCounts".equals(query.getPath()))
                return new JsonListMap<ActivityCount>(json, ActivityCount.class);
            else
                throw new JSONException("Unsupported export type for query: " + query);
        }
        catch (ParseException ex) {
            throw new JSONException(ex);
        }
    }
}
