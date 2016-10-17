/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.action;

import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.process.ProcessList;
import com.centurylink.mdw.services.ProcessServices;
import com.centurylink.mdw.services.ServiceLocator;

public class DeleteProcessInstances implements JsonService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public String getJson(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        try {
            String procId = (String)parameters.get("processId");
            if (procId != null) {
                ProcessServices processServices = ServiceLocator.getProcessServices();
                int count = processServices.deleteProcessInstances(new Long(procId));
                if (logger.isDebugEnabled())
                    logger.debug("Deleted " + count + " process instances for process id: " + procId);
            }
            else {
                JSONObject jsonObject = (JSONObject) parameters.get(ProcessList.PROCESS_INSTANCES);
                if (jsonObject == null)
                    throw new ServiceException("Missing parameter: either 'processId' or '" + ProcessList.PROCESS_INSTANCES + "' required.");
                ProcessList processList = new ProcessList(ProcessList.PROCESS_INSTANCES, jsonObject);
                ProcessServices processServices = ServiceLocator.getProcessServices();
                processServices.deleteProcessInstances(processList);
                if (logger.isDebugEnabled())
                    logger.debug("Deleted " + processList.getCount() + " process instances");
            }

            return null;  // success
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        return getJson(parameters, metaInfo);
    }

}