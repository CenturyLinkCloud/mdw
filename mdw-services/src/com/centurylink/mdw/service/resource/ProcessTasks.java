/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.util.Map;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskServices;
import com.centurylink.mdw.task.types.TaskList;

public class ProcessTasks implements JsonService {

    public String getJson(Map<String,Object> parameters, Map<String,String> metaInfo)
    throws ServiceException {

        String processInstanceId = (String)parameters.get("processInstanceId");
        if (processInstanceId == null)
            throw new ServiceException("Missing parameter: processInstanceId");

        try {
            TaskServices taskServices = ServiceLocator.getTaskServices();
            TaskList taskList = taskServices.getProcessTasks(new Long(processInstanceId));
            return taskList.getJson().toString(2);
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo)
    throws ServiceException {
        return getJson(parameters, metaInfo);
    }
}
