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
package com.centurylink.mdw.slack;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.Comment;
import com.centurylink.mdw.monitor.ServiceMonitor;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.HttpHelper;

/**
 * Monitors for comment messages posted in MDW through its API(s).
 */
@RegisteredService(ServiceMonitor.class)
public class MessageMonitor implements ServiceMonitor {

    @Override
    public Object onRequest(Object request, Map<String,String> headers) throws ServiceException {
        // only POST currently (TODO: how to handle PUT, DELETE)
        if ("Comments".equals(headers.get("RequestPath")) && "POST".equalsIgnoreCase(headers.get("HttpMethod"))) {
            Comment comment = new Comment(new JSONObject(request.toString()));
            if ("TASK_INSTANCE".equals(comment.getOwnerType())) {
                Long instId = comment.getOwnerId();
                Map<String,String> indexes = ServiceLocator.getTaskServices().getIndexes(instId);
                String messageTs = indexes.get("slack:message_ts");
                if (messageTs != null) {
                    JSONObject json = new JSONObject();
                    json.put("thread_ts", indexes.get("slack:message_ts"));
                    json.put("reply_broadcast", true);
                    String text = new MarkdownScrubber(comment.getContent()).toSlack();
                    if (text.length() > 1024)
                        text = text.substring(0, 1021) + "...";
                    json.put("text", text);
                    json.put("channel", "C85DLE1U7"); // TODO
                    json.put("as_user", false);
                    try {
                        HttpHelper helper = new HttpHelper(new URL("https://slack.com/api/chat.postMessage"));
                        Map<String,String> hdrs = new HashMap<>();
                        hdrs.put("Authorization", "Bearer " + System.getenv("MDW_SLACK_AUTH_TOKEN"));
                        hdrs.put("Content-Type", "application/json; charset=utf-8");
                        helper.setHeaders(hdrs);
                        String response = helper.post(json.toString());
                        JSONObject responseJson = new JSONObject(response);
                        if (!responseJson.getBoolean("ok"))
                            throw new IOException("Slack notification failed with response:" + responseJson);
                    }
                    catch (IOException ex) {
                        throw new ServiceException(ex.getMessage(), ex);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Object onHandle(Object request, Map<String,String> headers) throws ServiceException {
        return null;
    }

    @Override
    public Object onResponse(Object response, Map<String,String> headers) throws ServiceException {
        return null;
    }

    @Override
    public Object onError(Throwable t, Map<String,String> headers) {
        return null;
    }
}
