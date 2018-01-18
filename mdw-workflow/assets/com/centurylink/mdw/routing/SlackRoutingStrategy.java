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
package com.centurylink.mdw.routing;

import java.net.URL;
import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.common.service.AbstractRoutingStrategy;
import com.centurylink.mdw.common.service.RequestRoutingStrategy;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.slack.SlackEvent;
import com.centurylink.mdw.slack.SlackRequest;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Routing strategy that determines the appropriate destination based on the round-robin algorithm.
 */
@RegisteredService(RequestRoutingStrategy.class)
public class SlackRoutingStrategy extends AbstractRoutingStrategy {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static final int priority = 10;

    public SlackRoutingStrategy() {
    }

    public int getPriority() {
        return priority;
    }

    public URL getDestination(Object request, Map<String,String> headers) {

        String requestUrl = headers.get(Listener.METAINFO_REQUEST_URL);
        if (StringHelper.isEmpty(requestUrl) || !requestUrl.contains("com/centurylink/mdw/slack"))
            return null;

        try {
            String callbackId = null;
            JSONObject message = new JSONObject(request.toString());

            if (requestUrl.endsWith("event")) {
                SlackEvent event = new SlackEvent(message);
                callbackId = event.getCallbackId();
            }
            else {
                SlackRequest slackReq = new SlackRequest(message);
                callbackId = slackReq.getCallbackId();
            }

            if (callbackId == null) {
                logger.debug("Received message for Slack routing, but missing callback_id");
                return null;
            }
            else {
                String[] array = callbackId.split("://");
                if (array.length < 2 || array[1].indexOf("/") < 0) {
                    logger.debug("Received message for Slack routing, but malformed callback_id");
                    return null;
                }
                String instance = array[1].substring(0, array[1].indexOf("/"));
                message.remove("token");
                request = message.toString();
                return buildURL(headers, instance);
            }
        }
        catch (Throwable e) {
            logger.exception(null, "Received Slack message for routing, but could not parse out destination", e);
        }
        return null;
    }
}
