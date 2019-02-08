package com.centurylink.mdw.service.data;

import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.request.ServicePath;
import com.centurylink.mdw.service.api.MdwSwaggerCache;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import io.swagger.models.Swagger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Caches normalized request paths.  These are stored in response
 * documents (whereas full paths are stored in request documents).
 */
public class ServicePaths implements CacheService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static List<ServicePath> inboundPaths;
    private static List<ServicePath> outboundPaths;

    @Override
    public void refreshCache() {
        clearCache();
    }

    @Override
    public void clearCache() {
        inboundPaths = null;
        outboundPaths = null;
    }

    private static synchronized void load() {
        // default is enabled if dashboard package present
        boolean hasDashboard = PackageCache.getPackage("com.centurylink.mdw.dashboard") != null;
        if (!PropertyManager.getBooleanProperty("mdw.service.path.cache", hasDashboard)) {
            logger.info("ServicePathCache disabled");
            return;
        }

        inboundPaths = new ArrayList<>();
        Swagger swagger = MdwSwaggerCache.getSwagger("/");
        if (swagger != null) {
            for (String swaggerPath : swagger.getPaths().keySet()) {
                inboundPaths.add(new ServicePath(swaggerPath));
            }
            Collections.sort(inboundPaths);
        }

        outboundPaths = new ArrayList<>();
        // TODO: load outbound swagger paths

    }

    public static String getInboundResponsePath(Map<String,String> meta) {
        if (inboundPaths == null)
            load();
        String url = meta.get(Listener.METAINFO_REQUEST_PATH);
        if (url == null)
            return null;
        return new ServicePath(url, meta.get(Listener.METAINFO_HTTP_METHOD)).normalize(inboundPaths).toString();
    }

    public static String getOutboundResponsePath(String url, String method) {
        if (outboundPaths == null)
            load();
        if (url == null)
            return null;
        return new ServicePath(url, method).normalize(outboundPaths).toString();
    }
}
