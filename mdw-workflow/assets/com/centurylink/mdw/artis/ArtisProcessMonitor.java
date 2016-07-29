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
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.process.ProcessRuntimeContext;
import com.centurylink.mdw.monitor.ProcessMonitor;
import com.qwest.artis.Artis;
import com.qwest.artis.RequestRecord;
import com.qwest.artis.Status;

@RegisteredService(ProcessMonitor.class)
public class ArtisProcessMonitor implements java.io.Serializable,ProcessMonitor {

    private static final Log LOG = LogFactory.getLog(ArtisProcessMonitor.class);
    private Artis artis = null;
    private boolean forceAll = false;
    private boolean forceWithConfig = false;

    public ArtisProcessMonitor() throws PropertyException
    {
        ArtisContainer artisContainer=ArtisContainer.getArtisContainer();

        this.artis = artisContainer.getArtis();
        this.forceAll = artisContainer.isForceAll();
        this.forceWithConfig = artisContainer.isForceWithConfig();

        LOG.trace("ArtisContainer: forceAll: " + artisContainer.isForceAll() + " forceWithConfig:" + artisContainer.isForceWithConfig());
        LOG.trace("In ArtisProcessMonitor: ArtisKey=" + artis.getArtisKey());
    }

    @Override
    public Map<String,Object> onFinish(ProcessRuntimeContext context) {

        LOG.trace("In ArtisProcessMonitor.onFinish - " + context.getProcess().getName());

        ArtisRecordCache artisCache = ArtisMonitorUtil.getArtisRecordCacheFromVariables(context.getVariables());

        ArtisProcessVariables processVariables = artisCache.getArtisProcessVariables();

        if (processVariables.getUseArtis())
        {
            if (artisCache != null)
                artis.asyncRequestStop(((ArtisRecordCache) artisCache).getRequestRecord(), Status.SUCCESS);
        }
        return null;
    }

    @Override
    public Map<String, Object> onStart(ProcessRuntimeContext context) {

        LOG.trace("In ArtisProcessMonitor.onStart");

        Map<String, Object> response = new HashMap<String, Object>();

        ArtisProcessVariables processVariables = new ArtisProcessVariables(context, artis, this.forceAll, this.forceWithConfig);

        if (processVariables.getUseArtis())
        {
            response = processArtis(context, processVariables);
        }

        return response;
    }

    private Map<String, Object> processArtis(ProcessRuntimeContext context, ArtisProcessVariables processVariables) {
        Map<String, Object> response = new HashMap<String, Object>();

        ArtisRecordCache recordCache = new ArtisRecordCache();


        Map<String,String> requestHeaders = buildArtisMap(ArtisMonitorUtil.getRequestHeadersFromVariables(context.getVariables()));

        Map<String, String> artisHeaders = ArtisMonitorUtil.getArtisHeadersFromVariables(context.getVariables());

        RequestRecord requestRecord = null;

        /*
         * First check artisHeaders, this would be populated by a parent process and should take precedence
         * Second check requestHeaders, this would be passed in from a calling system and should be used if they exist on the main process
         * Last use null as this is the start of the chain
         */

        String startInfo = processVariables.getStartInfo();
        if (artisHeaders != null && artisHeaders.size() > 0) {
            LOG.trace("Generating Artis Request Record using artisHeaders: " + StringHelper.makeFormattedString(null, artisHeaders));
            requestRecord = artis.getRequestRecord(artisHeaders,  processVariables.getServiceName(), processVariables.getFunctionLabel(), startInfo, false);
        }
        else if (requestHeaders != null && requestHeaders.size() > 0) {
            LOG.trace("Generating Artis Request Record using requestHeaders");
            requestRecord = artis.getRequestRecord(requestHeaders, processVariables.getServiceName(), processVariables.getFunctionLabel(), startInfo, false);
        }
        else {
            LOG.trace("Generating Artis Request Record using null: " + context.getMasterRequestId());
            requestRecord = artis.getRequestRecord((String) null, processVariables.getServiceName(), processVariables.getFunctionLabel(), startInfo, false);
        }

        /*
         * Currently you have to get the Async value from the Callout Record... So I call a dummy callout here
         * to generate the Async value and use that as the request record for storage.
         */
        /*
        CalloutRecord cr = artis.getCalloutRecord(requestRecord, "DummyForAsync", "DummyForAsync", startInfo);
        cr.start();
        String asyncValue = cr.getAsyncValue();
        cr.stop(Status.SUCCESS);
        */

        String asyncValue = requestRecord.getAsyncValue();

        recordCache.setRequestRecord(asyncValue);
        recordCache.setArtisProcessVariables(processVariables);

        response.put(MDWArtisConstants.ARTISCACHE, recordCache);

        /*
         * MDW will not allow you to update an Input Variable when values were passed in to the process
         * So you can only update the ArtisHeaders if they were not bound from a parent process already
         */
        if (artisHeaders == null) {
            /*
             * TODO: Should I use the requestHeaders if that was passed in...
             */
            if (requestHeaders == null || requestHeaders.size() < 1)
                response.put(MDWArtisConstants.ARTISHEADERS, requestRecord.getArtisIds());
            else
                response.put(MDWArtisConstants.ARTISHEADERS, requestHeaders);
        }

        return response;
    }

    private Map<String, String> buildArtisMap(Map<String, String> requestHeaders) {
        Map<String, String> artisMap = new HashMap<String, String>();

        //TODO: Should probably do some type checking to ensure everything is insatnaceof String at runtime, Assuming this is true for now
        if (requestHeaders != null && requestHeaders.size() > 0) {
            for (String key:requestHeaders.keySet()) {
                LOG.trace("buildArtisMap: " + key + "/" + requestHeaders.get(key));

                if (key.startsWith("Artis")) {
                    artisMap.put(key, requestHeaders.get(key));
                }
            }
        }

        return artisMap;
    }

}