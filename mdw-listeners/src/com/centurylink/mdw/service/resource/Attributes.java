/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.services.ServiceLocator;

public class Attributes implements JsonService {

    public String getJson(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        String ownerType = (String)parameters.get("ownerType");
        if (ownerType == null)
            throw new ServiceException("Missing parameter: ownerType");
        String ownerId = (String)parameters.get("ownerId");
        if (ownerId == null)
            throw new ServiceException("Missing parameter: ownerId");

        try {
            Map<String,String> attrs = ServiceLocator.getWorkflowServices().getAttributes(ownerType, Long.parseLong(ownerId));
            JSONObject attrsJson = new JSONObject();
            for (String name : attrs.keySet())
                attrsJson.put(name, attrs.get(name));
            return attrsJson.toString(2);
        }
        catch (Exception ex) {
            throw new ServiceException("Error loading attributes for " + ownerType + ": " + ownerId, ex);
        }
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        return getJson(parameters, metaInfo);
    }
}
