package com.centurylink.mdw.service.data;

import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.request.ServicePath;
import com.centurylink.mdw.service.api.MdwSwaggerCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.yaml.YamlLoader;
import io.swagger.models.Swagger;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Caches normalized request paths.  These are stored in response
 * documents for aggregation purposes (whereas full paths are stored
 * in request documents).
 */
public class ServicePaths implements CacheService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static List<ServicePath> inboundPaths;
    public static List<ServicePath> getInboundPaths() { return inboundPaths; }

    private static List<ServicePath> outboundPaths;
    public static List<ServicePath> getOutboundPaths() { return outboundPaths; }

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
        List<ServicePath> inboundPathsTmp = inboundPaths;
        List<ServicePath> outboundPathsTmp;

        if (inboundPathsTmp == null) {

            inboundPathsTmp = new ArrayList<>();
            outboundPathsTmp = new ArrayList<>();

            // default is enabled if dashboard package present
            boolean hasDashboard = PackageCache.getPackage("com.centurylink.mdw.dashboard") != null;
            if (hasDashboard) {
                // inbound paths
                Swagger swagger = MdwSwaggerCache.getSwagger("/");
                if (swagger != null) {
                    for (String swaggerPath : swagger.getPaths().keySet()) {
                        inboundPathsTmp.add(new ServicePath(swaggerPath));
                    }
                    Collections.sort(inboundPathsTmp);
                }

                // outbound paths
                try {
                    Map<String, List<AssetInfo>> swaggers = ServiceLocator.getAssetServices().findAssets(file ->
                            file.getName().equals("swagger.yaml") || file.getName().equals("swagger.json")
                    );
                    for (String pkg : swaggers.keySet()) {
                        for (AssetInfo swaggerAsset : swaggers.get(pkg)) {
                            if (swaggerAsset.getExtension().equals("yaml")) {
                                YamlLoader yamlLoader = new YamlLoader(swaggerAsset.getFile());
                                Map yamlPaths = yamlLoader.getMap("paths", yamlLoader.getTop());
                                if (yamlPaths != null) {
                                    String basePath = yamlLoader.get("basePath", (Map) yamlLoader.getTop());
                                    final String base = basePath == null ? "" : basePath;
                                    yamlPaths.keySet().stream().forEach(p ->
                                            outboundPathsTmp.add(new ServicePath(base + p.toString()))
                                    );
                                }
                            } else if (swaggerAsset.getExtension().equals("json")) {
                                String content = new String(Files.readAllBytes(swaggerAsset.getFile().toPath()));
                                JSONObject json = new JSONObject(content);
                                if (json.has("paths")) {
                                    String base = json.optString("basePath");
                                    JSONObject pathsJson = json.getJSONObject("paths");
                                    for (String path : JSONObject.getNames(pathsJson)) {
                                        outboundPathsTmp.add(new ServicePath(base + path));
                                    }
                                }
                            }
                        }
                    }
                } catch (ServiceException | IOException ex) {
                    logger.severeException(ex.getMessage(), ex);
                }
            } else {
                logger.info("ServicePaths cache disabled");
            }
            inboundPaths = inboundPathsTmp;
            outboundPaths = outboundPathsTmp;
        }
    }

    public static String getInboundResponsePath(Map<String,String> meta) {
        if (inboundPaths == null)
            load();
        String path = meta.get(Listener.METAINFO_REQUEST_PATH);
        if (path == null)
            return null;
        return new ServicePath(path, meta.get(Listener.METAINFO_HTTP_METHOD)).normalize(inboundPaths).toString();
    }

    public static String getOutboundResponsePath(String url, String method) {
        if (outboundPaths == null)
            load();
        if (url == null)
            return null;
        try {
            return getOutboundResponsePath(new URL(url), method);
        }
        catch (MalformedURLException ex) {
            return new ServicePath(url, method).normalize(outboundPaths).toString();
        }
    }

    public static String getOutboundResponsePath(URL url, String method) {
        if (outboundPaths == null)
            load();
        if (url == null)
            return null;
        return new ServicePath(url.getPath(), method).normalize(outboundPaths).toString();
    }
}
