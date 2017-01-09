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

import com.centurylink.mdw.common.service.JsonArray;
import com.centurylink.mdw.common.service.JsonExportable;
import com.centurylink.mdw.common.service.JsonListMap;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessCount;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessList;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.JsonUtil;
import com.centurylink.mdw.util.StringHelper;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Processes")
@Api("MDW process instances and values")
public class Processes extends JsonRestService implements JsonExportable {

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.ProcessInstance;
    }

    /**
     * Retrieve process instance(s).
     */
    @Override
    @Path("/{instanceId|special}/{subData}/{subId}")
    @ApiOperation(value="Retrieve a process or process values, query many processes, or perform throughput queries",
        notes="If instanceId and special are not present, returns a page of processes that meet query criteria. "
          + "If {subData} is 'values', and then {subId} can be varName or expression (otherwise all populated values are returned). "
          + "If {subData} is 'summary' then a only summary-level process info is returned.",
        response=ProcessInstance.class, responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers)
    throws ServiceException, JSONException {
        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        try {
            String segOne = getSegment(path, 1);
            if (segOne != null) {
                try {
                    long instanceId = Long.parseLong(segOne);
                    String segTwo = getSegment(path, 2);
                    if ("values".equalsIgnoreCase(segTwo)) {
                        String varName = getSegment(path, 3);
                        if (varName != null) {
                            // individual value
                            Value value = workflowServices.getProcessValue(instanceId, varName);
                            return value.getJson();
                        }
                        else {
                            // all values
                            Map<String,Value> values = workflowServices.getProcessValues(instanceId);
                            JSONObject valuesJson = new JSONObject();
                            for (String name : values.keySet()) {
                                valuesJson.put(name, values.get(name).getJson());
                            }
                            return valuesJson;
                        }
                    }
                    else if ("summary".equalsIgnoreCase(segTwo)) {
                        ProcessInstance process = workflowServices.getProcess(instanceId, false);
                        JSONObject summary = new JSONObject();
                        summary.put("id", process.getId());
                        summary.put("name", process.getProcessName());
                        summary.put("packageName", process.getPackageName());
                        summary.put("version", process.getProcessVersion());
                        summary.put("masterRequestId", process.getMasterRequestId());
                        return summary;
                    }
                    else {
                        JSONObject json = workflowServices.getProcess(instanceId, true).getJson();
                        json.put("retrieveDate", StringHelper.serviceDateToString(DatabaseAccess.getDbDate()));
                        return json;
                    }
                }
                catch (NumberFormatException ex) {
                    // path must be special
                    Query query = getQuery(path, headers);
                    if (segOne.equals("definitions")) {
                        List<Process> processVOs = workflowServices.getProcessDefinitions(query);
                        JSONArray jsonProcesses = new JSONArray();
                        for (Process processVO : processVOs) {
                            JSONObject jsonProcess = new JSONObject();
                            jsonProcess.put("packageName", processVO.getPackageName());
                            jsonProcess.put("processId", processVO.getId());
                            jsonProcess.put("name", processVO.getName());
                            jsonProcess.put("version", processVO.getVersionString());
                            jsonProcesses.put(jsonProcess);
                        }
                        return new JsonArray(jsonProcesses).getJson();
                    }
                    else if (segOne.equals("topThroughput")) {
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
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    @Override
    @Path("/{instanceId}/values")
    @ApiOperation(value="Update value(s) for a process instance",
        notes="Values are created or updated based on the passed JSON object.", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Values", paramType="body", dataType="java.util.Map")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        String id = getSegment(path, 1);
        if (id == null)
            throw new ServiceException(HTTP_400_BAD_REQUEST, "Missing path segment: {instanceId}");
        try {
            Long instanceId = Long.parseLong(id);
            if ("values".equals(getSegment(path, 2))) {
                Map<String,String> values = JsonUtil.getMap(content);
                WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
                for (String varName : values.keySet()) {
                    workflowServices.setVariable(instanceId, varName, values.get(varName));
                }
                return null;
            }
            else {
                throw new ServiceException(HTTP_400_BAD_REQUEST, "Missing path segment: values");
            }
        }
        catch (NumberFormatException ex) {
            throw new ServiceException(HTTP_400_BAD_REQUEST, "Invalid instance id: " + id);
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
