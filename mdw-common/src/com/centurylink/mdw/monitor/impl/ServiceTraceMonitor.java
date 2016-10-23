/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.monitor.impl;

import java.util.Map;

import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.monitor.ServiceMonitor;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class ServiceTraceMonitor implements ServiceMonitor {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public Object onRequest(Object request, Map<String,String> headers) {
        StringBuffer sb = new StringBuffer();
        sb.append("\nSERVICE REQUEST:\n---------------\n");
        sb.append("request path: " + headers.get(Listener.METAINFO_REQUEST_PATH)).append("\n");
        sb.append("request:\n========\n" + request).append("\n");
        logger.info(sb.toString());
        return null;
    }

    public Object onHandle(Object request, Map<String,String> headers) {
        return null;
    }

    public Object onResponse(Object response, Map<String,String> headers) {
        StringBuffer sb = new StringBuffer();
        sb.append("\nSERVICE RESPONSE:\n---------------\n");
        sb.append("response:\n=========\n" + response).append("\n");
        logger.info(sb.toString());
        return null;
    }

    public Object onError(Throwable t, Map<String,String> headers) {
        return null;
    }

}
