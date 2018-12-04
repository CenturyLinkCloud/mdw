package com.centurylink.mdw.kubernetes;

import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.util.DesignatedHostSslVerifier;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.centurylink.mdw.kubernetes.Context.K8S_NAMESPACE;
import static com.centurylink.mdw.kubernetes.Context.K8S_SERVICE_TOKEN;

public class Invoker {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    void initialize() throws IOException {
        if (Context.getNamespace() == null) {
            throw new IOException(K8S_NAMESPACE + " not found");
        }
        if (Context.getServiceToken() == null) {
            throw new IOException(K8S_SERVICE_TOKEN + " not found");
        }
        try {
            DesignatedHostSslVerifier.setupSslVerification(Context.getApiHost());
            DesignatedHostSslVerifier.setupSslVerification(Context.INTERNAL_HOST);
        }
        catch (Exception ex) {
            logger.severeException("Error initializing SSL; invocations are unlikely to succeed", ex);
        }
    }

    public String invoke(String path) throws IOException {
        initialize();
        if (!path.startsWith("/"))
            path = "/" + path;
        String url = Context.getBaseUrl() + path;
        HttpHelper helper = new HttpHelper(new URL(url));
        Map<String,String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + Context.getServiceToken());
        helper.setHeaders(requestHeaders);
        return helper.get();
    }

}
