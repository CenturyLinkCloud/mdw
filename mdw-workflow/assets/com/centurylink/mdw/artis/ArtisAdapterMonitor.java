/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.artis;
import java.util.Map;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.model.value.activity.ActivityRuntimeContext;
import com.centurylink.mdw.monitor.AdapterMonitor;

@RegisteredService(AdapterMonitor.class)
public class ArtisAdapterMonitor implements java.io.Serializable,AdapterMonitor {

    @Override
    public Object onRequest(ActivityRuntimeContext runtimeContext, Object request, Map<String,String> headers) {
        return null;
    }


    @Override
    public Object onInvoke(ActivityRuntimeContext runtimeContext, Object request, Map<String,String> headers) {
        /*
         * TODO: What headers should be passed here? Should it be the parents ArtisHeaders or the ArtisHeaders of
         * the current process? If it needs to be the current process then I will need to store those headers in the
         * ArtisRecordsCache and retrieve them from there rather then from the ArtisHeaders variable. Also, may need to
         * Look at subProcess Bindings....
         */
        if (runtimeContext.getVariables().containsKey(MDWArtisConstants.ARTISHEADERS)) {
            @SuppressWarnings("unchecked")
            Map<String,String> artisHeaders = (Map<String,String>)runtimeContext.getVariables().get(MDWArtisConstants.ARTISHEADERS);
            for (String key : artisHeaders.keySet())
                headers.put(key, artisHeaders.get(key));
        }

        return null;
    }

    @Override
    public Object onResponse(ActivityRuntimeContext runtimeContext, Object response, Map<String,String> headers) {
        return null;
    }

    @Override
    public String onError(ActivityRuntimeContext runtimeContext, Throwable t) {
        return null;
    }
}