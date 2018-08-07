package com.centurylink.mdw.services.util;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.net.SocketTimeoutException;
import java.net.URL;

/**
 * After all caches have been loaded or refreshed (startup or a cache refresh request),
 * submit a request to eliminate "first request" performance penalty.
 */
public class InitialRequest {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private String path;

    public InitialRequest() {
        this("services/AppSummary");
    }

    public InitialRequest(String path) {
        this.path = path.startsWith("/") ? path.substring(1) : path;
    }

    public void submit() {
        try {
            String url = ApplicationContext.getLocalServiceAccessUrl() + "/" + path;
            logger.info("Submit initial request: " + url);
            HttpHelper helper = new HttpHelper(new URL(url));
            helper.setConnectTimeout(1000);
            helper.setReadTimeout(1000);
            helper.get();
        }
        catch (SocketTimeoutException ex) {
            // no need to wait for response
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }
}
