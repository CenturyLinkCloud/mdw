package com.centurylink.mdw.msteams;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.cache.asset.AssetCache;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.observer.ObserverException;
import com.centurylink.mdw.observer.task.TemplatedNotifier;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@RegisteredService(com.centurylink.mdw.observer.task.TaskNotifier.class)
public class TaskNotifier extends TemplatedNotifier {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

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
            String webhookUrl = getWebhookUrl();
            HttpHelper httpHelper = HttpHelper.getHttpHelper("POST", new URL(webhookUrl));
            httpHelper.setHeaders(getRequestHeaders());
            String response = httpHelper.post(getMessage().toString(2));
            if (httpHelper.getResponseCode() != 200) {
                String msg = httpHelper.getResponseCode() + " response from " + webhookUrl;
                logger.error(msg + ":\n" + response);
                throw new ObserverException(msg);
            }
        }
        catch (IOException ex) {
            throw new ObserverException(ex.getMessage(), ex);
        }
    }

    public String getWebhookUrl() throws IOException {
        String url = PropertyManager.getProperty("mdw.msteams.webhook.url");
        if (url == null)
            throw new IOException("Missing configuration: mdw.msteams.webhook.url");
        return url;
    }

    protected Map<String,String> getRequestHeaders() {
        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    protected JSONObject getMessage() throws IOException {
        Asset template = AssetCache.getAsset(getTemplateSpec());
        if (template == null)
            throw new IOException("Missing template: " + getTemplateSpec());
        String message = context.evaluateToString(template.getText());
        return new JsonObject(message);
    }
}
