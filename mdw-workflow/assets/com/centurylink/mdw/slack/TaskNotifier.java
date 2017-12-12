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

import org.json.JSONObject;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.observer.ObserverException;
import com.centurylink.mdw.observer.task.TemplatedNotifier;
import com.centurylink.mdw.util.HttpHelper;

@RegisteredService(com.centurylink.mdw.observer.task.TaskNotifier.class)
public class TaskNotifier extends TemplatedNotifier {

    private TaskRuntimeContext context;

    @Override
    public void sendNotice(TaskRuntimeContext context, String taskAction, String outcome)
            throws ObserverException {
        // avoid nuisance notice to claimer and releaser
        if (TaskAction.CLAIM.equals(taskAction) || TaskAction.RELEASE.equals(taskAction)) {
            return;
        }

        this.context = context;

        try {
            HttpHelper helper = new HttpHelper(new URL(getWebhookUrl()));
            String response = helper.post(getMessage().toString(2));
            if (helper.getResponseCode() != 200)
                throw new ServiceException(helper.getResponseCode(), "Slack notification failed with response:" + response);
        }
        catch (Exception ex) {
            throw new ObserverException(ex.getMessage(), ex);
        }
    }

    /**
     * TODO: no need if the mdw slack app is installed
     */
    public String getWebhookUrl() throws PropertyException {
        String url = PropertyManager.getProperty("mdw.slack.webhook.url");
        if (url == null)
            throw new PropertyException("Missing configuration: mdw.slack.webhook.url");
        return url;
    }

    public JSONObject getMessage() {
        Asset template = AssetCache.getAsset(getTemplateSpec());
        String message = context.evaluateToString(template.getStringContent());
        JSONObject json;
        if (template.getLanguage().equals(Asset.JSON)) {
            json = new JSONObject(message);
        }
        else {
            json = new JSONObject();
            json.put("text", message);
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

}
