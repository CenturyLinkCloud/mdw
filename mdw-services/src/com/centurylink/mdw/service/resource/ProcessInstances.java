/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.value.process.LinkedProcessInstance;
import com.centurylink.mdw.model.value.process.ProcessList;
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

    public String getJson(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {

        try {
            ProcessServices processServices = ServiceLocator.getProcessServices();
            Object callHierarchyFor = parameters.get("callHierarchyFor");
            if (callHierarchyFor != null) {
                long instanceId = Long.parseLong(callHierarchyFor.toString());
                LinkedProcessInstance linkedInstance = processServices.getCallHierearchy(instanceId);
                return linkedInstance.getJson().toString(2);
            }
            else {
                Map<String,String> criteria = getCriteria(parameters);
                Map<String,String> variables = getVariables(parameters);

                int pageIndex = parameters.get("pageIndex") == null ? 0 : Integer.parseInt((String)parameters.get("pageIndex"));
                int pageSize = parameters.get("pageSize") == null ? 0 : Integer.parseInt((String)parameters.get("pageSize"));
                String orderBy = (String)parameters.get("orderBy");

                ProcessList procList = processServices.getInstances(criteria, variables, pageIndex, pageSize, orderBy);
                return procList.getJson().toString(2);
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        return getJson(parameters, metaInfo);
    }

    private Map<String,String> getCriteria(Map<String,Object> params) {
        Map<String,String> criteria = new HashMap<String,String>();
        for (String key : params.keySet()) {
            if (processParams.contains(key))
                criteria.put(key, (String)params.get(key));
        }
        return criteria.isEmpty() ? null : criteria;
    }

    private Map<String,String> getVariables(Map<String,Object> params) {
        Map<String,String> variables = new HashMap<String,String>();
        for (String key : params.keySet()) {
            if (!processParams.contains(key) && !standardParams.contains(key)) {
                variables.put(key, (String)params.get(key));
            }
        }
        return variables.isEmpty() ? null : variables;
    }

}
