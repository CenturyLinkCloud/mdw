/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.bam;

import java.util.Map;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.data.work.WorkStatus;
import com.centurylink.mdw.model.value.activity.ActivityRuntimeContext;
import com.centurylink.mdw.monitor.ActivityMonitor;
import com.centurylink.mdw.monitor.OfflineMonitor;
import com.centurylink.mdw.services.bam.BamSender;

@RegisteredService(com.centurylink.mdw.monitor.ActivityMonitor.class)
public class BamActivityMonitor implements ActivityMonitor, OfflineMonitor<ActivityRuntimeContext> {

    public boolean handlesEvent(ActivityRuntimeContext runtimeContext, String event) {
        if (event.equals(WorkStatus.LOGMSG_START))
            return runtimeContext.getAttribute(WorkAttributeConstant.BAM_START_MSGDEF) != null;
        else if (event.equals(WorkStatus.LOGMSG_COMPLETE))
            return runtimeContext.getAttribute(WorkAttributeConstant.BAM_FINISH_MSGDEF) != null;
        else
            return false;
    }

    public Map<String,Object> onStart(ActivityRuntimeContext runtimeContext) {
        new BamSender(runtimeContext).sendMessage(WorkAttributeConstant.BAM_START_MSGDEF,
        		isIncludeLiveViewData());
        return null; // no updates
    }

    @Override
    public String onExecute(ActivityRuntimeContext runtimeContext) {
        return null;
    }

    public Map<String,Object> onFinish(ActivityRuntimeContext runtimeContext) {
        new BamSender(runtimeContext).sendMessage(WorkAttributeConstant.BAM_FINISH_MSGDEF,
        		isIncludeLiveViewData());
        return null;
    }

    public void onError(ActivityRuntimeContext runtimeContext) {
    }

    public boolean isIncludeLiveViewData() {
        return true;
    }

}