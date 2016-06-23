/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.process;

import java.util.Map;

import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.taskmgr.ui.process.FullProcessInstance;
import com.centurylink.mdw.taskmgr.ui.process.ProcessLaunchActionController;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class ProcessStartController extends ProcessLaunchActionController {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    protected void launchProcess(FullProcessInstance processInstance) {
        String masterRequestId = processInstance.getMasterRequestId();
        if (StringHelper.isEmpty(masterRequestId)) {
            FacesVariableUtil.addMessage("Master Request ID is required.");
            return;
        }
        try {
            String processName = ProcessVOCache.getProcessVO(processInstance.getProcessId()).getName();
            Map<String,Object> variables = processInstance.getVariables();
            Object launchDoc = getLaunchProcessDoc(processInstance, variables);
            ServiceLocator.getProcessManager().launchProcess(processName, launchDoc, masterRequestId, variables);
            FacesVariableUtil.addMessage("Process \"" + processInstance.getName() + "\" started.");
            auditLogProcessLaunch(processInstance);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            FacesVariableUtil.addMessage(ex.getMessage());
        }
    }
}
