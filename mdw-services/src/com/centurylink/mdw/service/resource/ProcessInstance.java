/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.services.ProcessServices;
import com.centurylink.mdw.services.ServiceLocator;

public class ProcessInstance implements JsonService {

    public String getJson(JSONObject request, Map<String,String> metaInfo) throws ServiceException {
        String instanceId = (String)metaInfo.get("instanceId");
        if (instanceId == null)
            instanceId = (String)metaInfo.get("processInstanceId");
        if (instanceId == null)
            throw new ServiceException("Missing parameter: instanceId");
        boolean shallow = "true".equals(metaInfo.get("shallow"));
        try {
            ProcessServices processServices = ServiceLocator.getProcessServices();
            com.centurylink.mdw.model.workflow.ProcessInstance processInstance;
            if (shallow)
                processInstance = processServices.getInstanceShallow(new Long(instanceId));
            else
                processInstance = processServices.getInstance(new Long(instanceId));

            // designer expects activity.statusMessage instead of activity.message
            JSONObject json = processInstance.getJson();
            if (json.has("activities")) {
                JSONArray activities = json.getJSONArray("activities");
                for (int i = 0; i < activities.length(); i++) {
                    JSONObject activity = activities.getJSONObject(i);
                    if (activity.has("message")) {
                        activity.put("statusMessage", activity.getString("message"));
                    }
                }
            }
            return json.toString(2);
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public String getText(Object obj, Map<String,String> metaInfo) throws ServiceException {
        return getJson((JSONObject)obj, metaInfo);
    }
}
