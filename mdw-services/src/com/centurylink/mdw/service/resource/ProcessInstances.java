/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.workflow.LinkedProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessList;
import com.centurylink.mdw.services.ProcessServices;
import com.centurylink.mdw.services.ServiceLocator;

public class ProcessInstances implements JsonService {

    private static List<String> processParams = Arrays.asList(new String[] {
        "processId",
        "processIdList",
        "processName",
        "id",
        "ownerId",
        "owner",
        "masterRequestId",
        "masterRequestIdIgnoreCase",
        "statusCode",
        "startDateFrom",
        "startDatefrom",
        "startDateTo",
        "startDateto",
        "endDateFrom",
        "endDatefrom",
        "endDateTo",
        "endDateto" });

    private static List<String> standardParams = Arrays.asList(new String[] {
        "pageIndex",
        "pageSize",
        "orderBy",
        "format"});

    public String getJson(JSONObject request, Map<String,String> metaInfo) throws ServiceException {

        try {
            ProcessServices processServices = ServiceLocator.getProcessServices();
            Object callHierarchyFor = metaInfo.get("callHierarchyFor");
            if (callHierarchyFor != null) {
                long instanceId = Long.parseLong(callHierarchyFor.toString());
                LinkedProcessInstance linkedInstance = processServices.getCallHierearchy(instanceId);
                return linkedInstance.getJson().toString(2);
            }
            else {
                Map<String,String> criteria = getCriteria(metaInfo);
                Map<String,String> variables = null; // TODO getVariables(metaInfo);

                int pageIndex = metaInfo.get("pageIndex") == null ? 0 : Integer.parseInt((String)metaInfo.get("pageIndex"));
                int pageSize = metaInfo.get("pageSize") == null ? 0 : Integer.parseInt((String)metaInfo.get("pageSize"));
                String orderBy = (String)metaInfo.get("orderBy");

                ProcessList procList = processServices.getInstances(criteria, variables, pageIndex, pageSize, orderBy);
                return procList.getJson().toString(2);
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public String getText(Object requestObj, Map<String,String> metaInfo) throws ServiceException {
        return getJson((JSONObject)requestObj, metaInfo);
    }

    private Map<String,String> getCriteria(Map<String,String> params) {
        Map<String,String> criteria = new HashMap<String,String>();
        for (String key : params.keySet()) {
            if (processParams.contains(key))
                criteria.put(key, (String)params.get(key));
        }
        return criteria.isEmpty() ? null : criteria;
    }

    private Map<String,String> getVariables(Map<String,String> params) {
        Map<String,String> variables = new HashMap<String,String>();
        for (String key : params.keySet()) {
            if (!processParams.contains(key) && !standardParams.contains(key)) {
                variables.put(key, (String)params.get(key));
            }
        }
        return variables.isEmpty() ? null : variables;
    }

}
