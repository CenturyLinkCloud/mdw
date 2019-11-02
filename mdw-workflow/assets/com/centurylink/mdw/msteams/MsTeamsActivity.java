package com.centurylink.mdw.msteams;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.NotificationActivity;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Send an MS Teams notification via webhook.
 */
@Activity(value="MS Teams Notify", category=NotificationActivity.class, icon="com.centurylink.mdw.msteams/msteams.png",
        pagelet="com.centurylink.mdw.msteams/msteams.pagelet")
public class MsTeamsActivity extends DefaultActivityImpl implements NotificationActivity {

    static final String WEBHOOK_URL = "webhookUrl";
    static final String MESSAGE = "message";
    static final String CONTINUE_DESPITE_MESSAGING_EXCEPTION = "continueDespiteMessagingException";

    public Object execute(ActivityRuntimeContext runtimeContext) throws ActivityException {
        sendNotices();
        return null;
    }

    @Override
    public void sendNotices() throws ActivityException {
        try {
            String webhookUrl = getWebhookUrl();
            HttpHelper httpHelper = HttpHelper.getHttpHelper("POST", new URL(webhookUrl));
            httpHelper.setHeaders(getRequestHeaders());
            String response = httpHelper.post(getMessage().toString(2));
            if (httpHelper.getResponseCode() != 200) {
                String msg = httpHelper.getResponseCode() + " response from " + webhookUrl;
                logError(msg + ":\n" + response);
                throw new IOException(msg);
            }
        }
        catch (IOException ex) {
            logError(ex.getMessage(), ex);
            if (!getAttribute(CONTINUE_DESPITE_MESSAGING_EXCEPTION, false))
                throw new ActivityException(ex.getMessage(), ex);
        }
    }

    protected String getWebhookUrl() throws ActivityException {
        return getRequiredAttribute(WEBHOOK_URL);
    }

    protected Map<String,String> getRequestHeaders() {
        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    protected JSONObject getMessage() throws ActivityException {
        String messageAsset = getRequiredAttribute(MESSAGE);
        String assetVersion = getAttribute(MESSAGE + "_assetVersion");
        AssetVersionSpec assetSpec = new AssetVersionSpec(messageAsset, assetVersion == null ? "0" : assetVersion);
        Asset asset = AssetCache.getAsset(messageAsset);
        if (asset == null)
            throw new ActivityException("Asset not found: " + assetSpec);
        return new JsonObject(getRuntimeContext().evaluateToString(asset.getStringContent()));
    }
}
