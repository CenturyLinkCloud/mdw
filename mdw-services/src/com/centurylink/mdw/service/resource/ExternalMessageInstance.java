/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.util.Map;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.value.event.ExternalMessageVO;
import com.centurylink.mdw.services.ExternalMessageServices;
import com.centurylink.mdw.services.ServiceLocator;

public class ExternalMessageInstance implements JsonService {

    public String getJson(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        String activityId = (String)parameters.get("activityId");
        if (activityId == null)
            throw new ServiceException("Missing parameter: activityId");
        String activityInstId = (String)parameters.get("activityInstId");
        if (activityInstId == null)
            throw new ServiceException("Missing parameter: activityInstId");
        String eventInstId = (String)parameters.get("eventInstId");
        try {
            ExternalMessageServices extMessageServices = ServiceLocator.getExternalMessageServices();
            ExternalMessageVO externalMessageVO;
            externalMessageVO = extMessageServices.getExternalMessage(new Long(activityId), new Long(activityInstId), eventInstId == null ? null : new Long(eventInstId));
            return externalMessageVO.getJson().toString(2);
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        return getJson(parameters, metaInfo);
    }
}
