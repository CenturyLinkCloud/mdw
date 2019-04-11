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
package com.centurylink.mdw.common.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * General strategy class to be inherited by all specific routing strategies created.
 */
public abstract class AbstractRoutingStrategy implements RequestRoutingStrategy {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static volatile List<String> activeServerList = new ArrayList<String>();
    private static volatile long lastCheck = 0;

    public String getStrategyName() {
        return this.getClass().getName();
    }

    protected List<String> getWorkerInstances() {
        return ApplicationContext.getServerList().getHostPortList();
    }

    protected List<String> getRoutingInstances() {
        return ApplicationContext.getRoutingServerList().getHostPortList();
    }

    protected String getServicesRoot() {
        return ApplicationContext.getServicesContextRoot();
    }

    /**
     * Override this to modify how active instances are determined
     */
    protected synchronized List<String> getActiveWorkerInstances() {
        List<String> localList = new ArrayList<String>();
        int activeServerInterval = PropertyManager.getIntegerProperty(PropertyNames.MDW_ROUTING_ACTIVE_SERVER_INTERVAL, 15);   // Represents how many seconds between checks

        if (lastCheck == 0 || ((System.currentTimeMillis() - lastCheck)/1000) > activeServerInterval) {
            lastCheck = System.currentTimeMillis();
            activeServerList.clear();
            for(String server : getWorkerInstances()) {
                try {
                    HttpHelper helper = new HttpHelper(new URL("http://" + server + "/" + ApplicationContext.getServicesContextRoot() + "/Services/SystemInfo?type=threadDumpCount&format=text"));
                    helper.setConnectTimeout(1000);     // Timeout after 1 second
                    helper.setReadTimeout(1000);
                    String response = helper.get();

                    if (!StringHelper.isEmpty(response) && Integer.parseInt(response) > 0 && !activeServerList.contains(server))
                        activeServerList.add(server);
                }
                catch (Exception ex) {
                    if (activeServerList.contains(server))
                        activeServerList.remove(server);

                    logger.info("MDW Server instance " + server + " is not responding");
                }
            }
            lastCheck = System.currentTimeMillis();
        }

        // If all worker instances are non-responsive, still try to send it to one of them - Don't want routing instance to process request
        // Could be they just did a cache refresh or initial startup and couldn't respond in < 1 second - First request
        if (activeServerList.isEmpty()) {
            for (String server : getWorkerInstances())
                activeServerList.add(server);
        }

        for (String server : activeServerList)
            localList.add(server);

        return localList;
    }

    /**
     * Override this to build the destination URL differently.
     */
    protected URL buildURL(Map<String,String> headers, String instanceHostPort) throws MalformedURLException {
        String origUrl = headers.get(Listener.METAINFO_REQUEST_URL);
        if (origUrl == null || !origUrl.startsWith("http")) {
            // other protocol: forward to standard services url
            return new URL("http://" + instanceHostPort + "/" + getServicesRoot() + "/Services");
        }
        else {
            URL origHttpUrl = new URL(origUrl);
            String path = origHttpUrl.getPath();
            // Re-route from SOAP to REST since MDW only cares about message Payload, plus we no longer have the SOAP Envelope in the request
            if (path != null && (path.contains("/SOAP") || path.contains("/MDWWebService")))
                path = "/" + getServicesRoot() + "/Services";

            String destUrl = origHttpUrl.getProtocol() + "://" + instanceHostPort + path;
            if (!StringHelper.isEmpty(headers.get(Listener.METAINFO_REQUEST_QUERY_STRING)))
                destUrl += "?" + headers.get(Listener.METAINFO_REQUEST_QUERY_STRING);
            return new URL(destUrl);
        }
    }
}
