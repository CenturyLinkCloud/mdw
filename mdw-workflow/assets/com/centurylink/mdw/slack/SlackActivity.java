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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

/**
 * A simple activity for sending slack Notification using webhook.
 * <p>
 * Posts any notice/exception message onto a Slack channel using webhook
 * </p>
 */
public class SlackActivity extends DefaultActivityImpl {

    public static String END_POINT_URI = "endpointUri";
    public static final String SLACK_MESSAGE = "slackMessage";
    // Prefix that identifies where the Slack message came from
    private static String SLACK_PREFIX = "slackPrefix";
    private ActivityRuntimeContext context;

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
     * if slackPrefix attribute is not set, it uses default "MDW-APP"
     */
    public String getSlackPrefix(){
        String slackPrefix=getAttributeValueSmart(SLACK_PREFIX);
        if(slackPrefix==null)
            slackPrefix="MDW-APP";
        return slackPrefix;
    }

    public JSONObject getMessage()throws ActivityException {
        String slackmessageAsset = getAttributeValueSmart(SLACK_MESSAGE);
        if(slackmessageAsset==null)
          throw new ActivityException("slack message is not set");
        Asset template = AssetCache.getAsset(slackmessageAsset);
        if(template==null)
            throw new ActivityException("Slack Message JSON asset could not be found "+slackmessageAsset);

        String message = context.evaluateToString(template.getStringContent());
        JSONObject json=new JSONObject();
        String env = ApplicationContext.getRuntimeEnvironment().toUpperCase();
        if (template.getLanguage().equals(Asset.JSON)) {
            json.put("text", env + " - " + getSlackPrefix() + message);

        }
        else {
            json = new JSONObject();
            json.put("text", env + " - " + getSlackPrefix() + message);
        }
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

    public String getWebhookUrl() throws ActivityException {
        String url = getAttributeValueSmart(END_POINT_URI);
        if (url == null)
            throw new ActivityException("Missing configuration: Slack End Point uri");
        return url;
    }

    @Override
    public Object execute(ActivityRuntimeContext context) throws ActivityException {
        try {
            this.context = context;
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
        return null;
    }

}
