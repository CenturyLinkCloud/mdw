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
package com.centurylink.mdw.workflow.task.notifier;

import java.io.IOException;
import java.net.URL;

import org.json.JSONObject;

import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.observer.ObserverException;
import com.centurylink.mdw.observer.task.TemplatedNotifier;
import com.centurylink.mdw.util.HttpHelper;

public class SlackNotifier extends TemplatedNotifier {

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
            helper.post(getMessage().toString(2));
        }
        catch (IOException ex) {
            throw new ObserverException(ex.getMessage(), ex);
        }
    }

    // TODO
    public String getWebhookUrl() {
        return "https://hooks.slack.com/services/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";
    }

    // TODO
    public JSONObject getMessage() {
        Asset template = AssetCache.getAsset(getTemplateSpec());
        String messageText = context.evaluateToString(template.getStringContent());
        // TODO: parameterization
        System.out.println("SUBSTITUTED: " + messageText);
        JSONObject json = new JSONObject();
        json.put("text", messageText);
        return json;
    }

}
