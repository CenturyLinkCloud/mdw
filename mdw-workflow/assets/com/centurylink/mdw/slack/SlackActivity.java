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
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import org.json.JSONObject;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple activity for sending slack Notification using webhook.
 * <p>
 * Posts any notice/exception message onto a Slack channel using webhook
 * </p>
 */
public class SlackActivity extends DefaultActivityImpl implements NotificationActivity {

    public static String END_POINT_URI = "endpointUri";
    public static final String SLACK_MESSAGE = "slackMessage";
    // Prefix that identifies where the Slack message came from
    private static String SLACK_PREFIX = "slackPrefix";

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
            if (!"true".equalsIgnoreCase(getAttributeValueSmart(
                    WorkAttributeConstant.CONTINUE_DESPITE_MESSAGING_EXCEPTION))) {
                throw new ActivityException(ex.getMessage(), ex);
            }
        }
    }

    /*
     * Make sure it's a JSON request
     */
    public Map<String, String> getRequestHeaders() {
        Map<String, String> hdrs = new HashMap<>();
        hdrs.put("Content-Type", "application/json");
        hdrs.put("environment", getSlackPrefix());
        return hdrs;
    }

    /*
     * if slackPrefix attribute is not set, it uses app name.
     */
    public String getSlackPrefix() {
        String slackPrefix = getAttributeValueSmart(SLACK_PREFIX);
        if (slackPrefix == null)
            slackPrefix = ApplicationContext.getAppId();
        return slackPrefix;
    }

    /*
     * The source of Slack message can be either Json asset or process variable.
     */
    public JSONObject getMessage() throws ActivityException {
        String message = null;
        String slackMessageName = getAttributeValueSmart(SLACK_MESSAGE);
        if (slackMessageName == null)
            throw new ActivityException("slack message attribute is not set");
        Asset template = AssetCache.getAsset(slackMessageName);

        if (template == null) {
            message = slackMessageName;
        }
        else {
            message = getRuntimeContext().evaluateToString(template.getStringContent());
        }

        JSONObject json = new JSONObject();
        String env = ApplicationContext.getRuntimeEnvironment().toUpperCase();
        json.put("text", env + " - " + getSlackPrefix() + " - " + message);

        String altText = null;
        if (json.has("text")) {
            String text = json.getString("text");
            if (text.length() > 200)
                altText = text.substring(0, 197) + "...";
        }
        if (altText != null)
            json.put("text", altText);
        return json;
    }

    /*
     * Reads Slack URL which is an environment variable configured as attribute
     * END_POINT_URI.
     */
    public String getWebhookUrl() throws ActivityException {
        String url = System.getenv(getAttributeValueSmart(END_POINT_URI));
        if (url == null)
            throw new ActivityException("Missing configuration: Slack End Point uri");
        return url;
    }

}
