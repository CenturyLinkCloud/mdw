/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
