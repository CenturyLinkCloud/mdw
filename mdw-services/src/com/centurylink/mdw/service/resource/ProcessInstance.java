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
