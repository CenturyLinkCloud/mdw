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
import com.centurylink.mdw.model.value.process.ProcessCount;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessList;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/Processes")
@Api("MDW process instances")
public class Processes extends JsonRestService implements Exportable {

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.ProcessInstance;
    }

    /**
     * Retrieve process instance(s).
     */
    @Override
    @Path("/{instanceId|special}")
    @ApiOperation(value="Retrieve a process, query many processes, or perform special queries",
        notes="If instanceId and special are not present, returns a page of processes that meet query criteria.",
        response=ProcessInstanceVO.class, responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers)
    throws ServiceException, JSONException {
        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        try {
            String segOne = getSegment(path, 1);
            if (segOne != null) {
                try {
                    long instanceId = Long.parseLong(segOne);
                    return workflowServices.getProcess(instanceId).getJson();
                }
                catch (NumberFormatException ex) {
                    // path must be special
                    Query query = getQuery(path, headers);
                    if (segOne.equals("topThroughput")) {
                        List<ProcessCount> list = workflowServices.getTopThroughputProcesses(query);
                        JSONArray procArr = new JSONArray();
                        int ct = 0;
                        ProcessCount other = null;
                        long otherTot = 0;
                        for (ProcessCount procCount : list) {
                            if (ct >= query.getMax()) {
                                if (other == null) {
                                    other = new ProcessCount(0);
                                    other.setName("Other");
                                }
                                otherTot += procCount.getCount();
                            }
                            else {
                                procArr.put(procCount.getJson());
                            }
                            ct++;
                        }
                        if (other != null) {
                            other.setCount(otherTot);
                            procArr.put(other.getJson());
                        }
                        return new JsonArray(procArr).getJson();
                    }
                    else if (segOne.equals("instanceCounts")) {
                        Map<Date,List<ProcessCount>> dateMap = workflowServices.getProcessInstanceBreakdown(query);
                        boolean isTotals = query.getFilters().get("processIds") == null && query.getFilters().get("statuses") == null;

                        Map<String,List<ProcessCount>> listMap = new HashMap<String,List<ProcessCount>>();
                        for (Date date : dateMap.keySet()) {
                            List<ProcessCount> procCounts = dateMap.get(date);
                            if (isTotals) {
                                for (ProcessCount procCount : procCounts)
                                    procCount.setName("Total");
                            }
                            listMap.put(Query.getString(date), procCounts);
                        }

                        return new JsonListMap<ProcessCount>(listMap).getJson();
                    }
                    else {
                        throw new ServiceException(ServiceException.BAD_REQUEST, "Unsupported path segment: " + segOne);
                    }
                }
            }
            else {
                Query query = getQuery(path, headers);
                return workflowServices.getProcesses(query).getJson();
            }
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public Jsonable toJsonable(Query query, JSONObject json) throws JSONException {
        try {
            if (json.has(ProcessList.PROCESS_INSTANCES))
                return new ProcessList(ProcessList.PROCESS_INSTANCES, json);
            else if ("Processes/instanceCounts".equals(query.getPath()))
                return new JsonListMap<ProcessCount>(json, ProcessCount.class);
            else
                throw new JSONException("Unsupported export type for query: " + query);
        }
        catch (ParseException ex) {
            throw new JSONException(ex);
        }
    }
}
