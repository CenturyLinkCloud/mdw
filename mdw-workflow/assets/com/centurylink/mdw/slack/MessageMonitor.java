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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.Note;
import com.centurylink.mdw.monitor.ServiceMonitor;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.HttpHelper;

/**
 * Monitors for comment messages posted in MDW through its API(s).
 */
@RegisteredService(ServiceMonitor.class)
public class MessageMonitor implements ServiceMonitor {

    private static Pattern PATH_PATTERN = Pattern.compile("Tasks/([0-9]+)/comments(/)?([0-9]+)?.*");

    @Override
    public Object onRequest(Object request, Map<String,String> headers) throws ServiceException {
        String path = headers.get("RequestPath");
        if (path != null && request != null) {  // PUT, POST, DELETE
            Matcher matcher = PATH_PATTERN.matcher(path);
            if (matcher.matches()) {
                Long instId = new Long(matcher.group(1));
                Note note = new Note(new JSONObject(request.toString()));
                Map<String,String> indexes = ServiceLocator.getTaskServices().getIndexes(instId);
                String slackResponseUrl = indexes.get("slack:response_url");
                if (slackResponseUrl != null) {
                    JSONObject json = new JSONObject();
                    json.put("response_type", "in_channel");
                    if (indexes.containsKey("slack:message_ts")) {
                        json.put("thread_ts", indexes.get("slack:message_ts"));
                        json.put("reply_broadcast", true);
                    }
                    // TODO: this updates the very original message, instead set thread_ts to reply ts (probably)
                    json.put("replace_original", "put".equalsIgnoreCase(headers.get("HttpMethod")));
                    String altText = null;
                    if (note.getContent().length() > 200)
                        altText = note.getContent().substring(0, 197) + "...";
                    json.put("text", altText == null ? note.getContent() : altText);
                    try {
                        HttpHelper helper = new HttpHelper(new URL(slackResponseUrl));
                        String response = helper.post(json.toString());
                        if (helper.getResponseCode() != 200)
                            throw new IOException("Slack notification failed with response:" + response);
                    }
                    catch (IOException ex) {
                        throw new ServiceException(ex.getMessage(), ex);
                    }
                }
            }
        }
        return null;
    }

    // TODO
    public String getWebhookUrl() {
        return "https://hooks.slack.com/services/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";
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
