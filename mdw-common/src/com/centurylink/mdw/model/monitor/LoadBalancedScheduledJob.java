package com.centurylink.mdw.model.monitor;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public abstract class LoadBalancedScheduledJob implements ScheduledJob {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public abstract void runOnLoadBalancedInstance(CallURL args);

    protected boolean runOnDifferentManagedServer(CallURL url, String remoteHostPort) {
        boolean isSuccess = true;
        // needs to be run on a different server instance
        String remoteUrl = "http://" + remoteHostPort + "/" + ApplicationContext.getServicesContextRoot() + "/services/ScheduledJobs/run";

        JSONObject json = new JSONObject();
        json.put("className", url.getAction());
        for (String key : url.getParameters().keySet()) {
            if (!StringUtils.isBlank(url.getParameter(key)))
                json.put(key, url.getParameter(key));
        }

        HttpHelper httpHelper = null;
        try {
            // submit the request
            httpHelper = new HttpHelper(new URL(remoteUrl));
            Map<String,String> hdrs = new HashMap<>();
            hdrs.put("Content-Type", "application/json");
            httpHelper.setHeaders(hdrs);
            String response = httpHelper.post(json.toString());
            if (httpHelper.getResponseCode() != 200) {
                logger.error("Response Status message from instance "+ remoteHostPort +" ScheduledJob."+this.getClass().getName()+" : "+ response);
            }
        }
        catch (IOException ex) {
            isSuccess = false; // instance is offline
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return isSuccess;
    }

}
