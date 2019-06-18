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

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.NotificationActivity;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import org.json.JSONObject;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Send a Slack notification via webhook.
 */
@Activity(value="Slack Notify", category=NotificationActivity.class, icon="com.centurylink.mdw.slack/slack.png",
        pagelet="com.centurylink.mdw.slack/slack.pagelet")
public class SlackActivity extends DefaultActivityImpl implements NotificationActivity {

    static final String WEBHOOK_URL = "webhookUrl";
    static final String MESSAGE = "message";
    static final String CONTINUE_DESPITE_MESSAGING_EXCEPTION = "continueDespiteMessagingException";

    @Override
    public Object execute(ActivityRuntimeContext runtimeContext) throws ActivityException {
        sendNotices();
        return null;
    }

    @Override
    public void sendNotices() throws ActivityException {
        try {
            Map<String, String> hdrs = getRequestHeaders();
            HttpHelper helper = new HttpHelper(new URL(getWebhookUrl()));
            helper.setHeaders(hdrs);
            String response = helper.post(getMessage().toString(2));
            if (helper.getResponseCode() != 200)
                throw new ServiceException(helper.getResponseCode(),
                        "Slack notification failed with response:" + response);
        }
        catch (Exception ex) {
            logexception(ex.getMessage(), ex);
            if (!getAttribute(CONTINUE_DESPITE_MESSAGING_EXCEPTION, false))
                throw new ActivityException(ex.getMessage(), ex);
        }
    }

    public Map<String,String> getRequestHeaders() {
        Map<String, String> hdrs = new HashMap<>();
        hdrs.put("Content-Type", "application/json");
        return hdrs;
    }

    public JSONObject getMessage() throws ActivityException {
        String messageAsset = getAttributeValueSmart(MESSAGE);
        if (messageAsset == null)
            messageAsset = getAttributeValueSmart("slackMessage");
        String assetVersion = getAttribute(MESSAGE + "_assetVersion");
        AssetVersionSpec assetSpec = new AssetVersionSpec(messageAsset, assetVersion == null ? "0" : assetVersion);
        Asset asset = AssetCache.getAsset(messageAsset);
        if (asset == null)
            throw new ActivityException("Asset not found: " + assetSpec);
        return new JsonObject(getRuntimeContext().evaluateToString(asset.getStringContent()));
    }

    public String getWebhookUrl() throws ActivityException {
        String url = getAttributeValueSmart(WEBHOOK_URL);
        if (url == null)
            url = System.getenv(getAttributeValueSmart("endpointUri"));
        if (url == null)
            throw new ActivityException("Missing required attribute: " + WEBHOOK_URL);
        return url;
    }
}
