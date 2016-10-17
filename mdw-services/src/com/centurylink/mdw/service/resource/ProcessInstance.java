/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.util.Map;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.services.ProcessServices;
import com.centurylink.mdw.services.ServiceLocator;

public class ProcessInstance implements JsonService {

    public String getJson(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        String instanceId = (String)parameters.get("instanceId");
        if (instanceId == null)
            instanceId = (String)parameters.get("processInstanceId");
        if (instanceId == null)
            throw new ServiceException("Missing parameter: instanceId");
        boolean shallow = "true".equals(parameters.get("shallow"));
        try {
            ProcessServices processServices = ServiceLocator.getProcessServices();
            ProcessInstanceVO processInstance;
            if (shallow)
                processInstance = processServices.getInstanceShallow(new Long(instanceId));
            else
                processInstance = processServices.getInstance(new Long(instanceId));
            return processInstance.getJson().toString(2);
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        return getJson(parameters, metaInfo);
    }
}
