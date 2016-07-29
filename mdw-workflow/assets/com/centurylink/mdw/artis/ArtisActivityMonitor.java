/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.artis;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.artis.util.ArtisMonitorUtil;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.model.value.activity.ActivityRuntimeContext;
import com.centurylink.mdw.monitor.ActivityMonitor;
import com.qwest.artis.Artis;
import com.qwest.artis.CalloutRecord;
import com.qwest.artis.Status;

@RegisteredService(ActivityMonitor.class)
public class ArtisActivityMonitor implements java.io.Serializable, ActivityMonitor {
    private static final Log LOG = LogFactory.getLog(ArtisActivityMonitor.class);
    Artis artis = null;
    private boolean forceAll = false;
    private boolean forceWithConfig = false;

    public ArtisActivityMonitor() throws PropertyException
    {
        ArtisContainer artisContainer = ArtisContainer.getArtisContainer();
        this.artis = artisContainer.getArtis();
        this.forceAll = artisContainer.isForceAll();
        this.forceWithConfig = artisContainer.isForceWithConfig();
        LOG.trace("ArtisContainer: forceAll: " + artisContainer.isForceAll() + " forceWithConfig:" + artisContainer.isForceWithConfig());
        LOG.trace("In ArtisActivityMonitor: ArtisKey=" + artis.getArtisKey());
    }

    @Override
    public String onExecute(ActivityRuntimeContext runtimeContext) {
        return null;
    }

    @Override
    public void onError(ActivityRuntimeContext context) {
        LOG.trace("In ArtisActivityMonitor.onError: " + context.getActivity().getActivityName());

        ArtisRecordCache artisCache = ArtisMonitorUtil.getArtisRecordCacheFromVariables(context.getVariables());

        ArtisProcessVariables processVariables = artisCache.getArtisProcessVariables();

        if (processVariables.getUseArtis())
        {
            if (artisCache.hasCalloutRecord(context.getActivityInstanceId().toString()))
            {
                artis.asyncCalloutStop(artisCache.useCalloutRecord(context.getActivityInstanceId().toString()), Status.FAILURE, "");
            }
        }
    }

    @Override
    public Map<String,Object> onFinish(ActivityRuntimeContext context) {
        LOG.trace("In ArtisActivityMonitor.onFinish: "  + context.getActivity().getActivityName());

        ArtisRecordCache artisCache = ArtisMonitorUtil.getArtisRecordCacheFromVariables(context.getVariables());
        ArtisProcessVariables processVariables = artisCache.getArtisProcessVariables();

        if (processVariables.getUseArtis())
        {
            if (artisCache.hasCalloutRecord(context.getActivityInstanceId().toString()))
            {
                artis.asyncCalloutStop(artisCache.useCalloutRecord(context.getActivityInstanceId().toString()), Status.SUCCESS, "");
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> onStart(ActivityRuntimeContext context) {
        LOG.trace("In ArtisActivityMonitor.onStart: " + context.getActivity().getActivityName());

        Map<String, Object> response = new HashMap<String, Object>();
        ArtisRecordCache artisCache = ArtisMonitorUtil.getArtisRecordCacheFromVariables(context.getVariables());

        ArtisProcessVariables processVariables = artisCache.getArtisProcessVariables();

        if (processVariables.getUseArtis())
        {
            ArtisProcessVariables activityVariables = new ArtisProcessVariables(context, processVariables, artis, forceAll, forceWithConfig);
            LOG.trace("activityVariables:" + activityVariables.toString());
            response = processArtis(artisCache, context, activityVariables);
        }

        return response;
    }

    private Map<String, Object> processArtis(ArtisRecordCache artisCache, ActivityRuntimeContext context, ArtisProcessVariables activityVariables)
    {
        LOG.trace("In ArtisActivityMonitor.processArtis: " + context.getActivity().getActivityName());

        Map<String, Object> response = new HashMap<String, Object>();

        if (activityVariables.getUseArtis())
        {
            CalloutRecord calloutRecord = artis.getCalloutRecordFromAsyncValue(artisCache.getRequestRecord(), activityVariables.getServiceName(), activityVariables.getFunctionLabel(), activityVariables.getStartInfo());
            calloutRecord.start();

            artisCache.addCalloutRecord(context.getActivityInstanceId().toString(), calloutRecord.getAsyncValue());

            response.put(MDWArtisConstants.ARTISCACHE, artisCache);
        }

        return response;
    }

}