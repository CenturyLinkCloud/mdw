/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.bam;

import java.util.Map;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.data.work.WorkStatus;
import com.centurylink.mdw.model.value.process.ProcessRuntimeContext;
import com.centurylink.mdw.monitor.OfflineMonitor;
import com.centurylink.mdw.monitor.ProcessMonitor;
import com.centurylink.mdw.services.bam.BamSender;

@RegisteredService(com.centurylink.mdw.monitor.ProcessMonitor.class)
public class BamProcessMonitor implements ProcessMonitor, OfflineMonitor<ProcessRuntimeContext> {

    public boolean handlesEvent(ProcessRuntimeContext runtimeContext, String event) {
        if (event.equals(WorkStatus.LOGMSG_PROC_START))
            return runtimeContext.getAttribute(WorkAttributeConstant.BAM_START_MSGDEF) != null;
        else if (event.equals(WorkStatus.LOGMSG_PROC_COMPLETE))
            return runtimeContext.getAttribute(WorkAttributeConstant.BAM_FINISH_MSGDEF) != null;
        else
            return false;
    }

    public Map<String,Object> onStart(ProcessRuntimeContext runtimeContext) {
        if (runtimeContext.getAttribute(WorkAttributeConstant.BAM_START_MSGDEF) != null)
            new BamSender(runtimeContext).sendMessage(WorkAttributeConstant.BAM_START_MSGDEF, isIncludeLiveViewData());
        return null;  // no updates
    }

    public Map<String,Object> onFinish(ProcessRuntimeContext runtimeContext) {
        if (runtimeContext.getAttribute(WorkAttributeConstant.BAM_FINISH_MSGDEF) != null)
            new BamSender(runtimeContext).sendMessage(WorkAttributeConstant.BAM_FINISH_MSGDEF, isIncludeLiveViewData());
        return null;
    }

    public void onError(ProcessRuntimeContext runtimeContext) {
    }

    public boolean isIncludeLiveViewData() {
        return true;
    }
}