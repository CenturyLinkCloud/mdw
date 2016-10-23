/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;

public class ExternalMessageInstance implements JsonService {

    public String getJson(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        String activityId = (String)parameters.get("activityId");
        if (activityId == null)
            throw new ServiceException("Missing parameter: activityId");
        String activityInstId = (String)parameters.get("activityInstId");
        if (activityInstId == null)
            throw new ServiceException("Missing parameter: activityInstId");

        // TODO: needed by Designer

        return new JSONObject().toString();
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        return getJson(parameters, metaInfo);
    }
}
