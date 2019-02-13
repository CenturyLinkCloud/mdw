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
        inboundPaths = new ArrayList<>();
        outboundPaths = new ArrayList<>();

        // default is enabled if dashboard package present
        boolean hasDashboard = PackageCache.getPackage("com.centurylink.mdw.dashboard") != null;
        if (hasDashboard) {
            // inbound paths
            Swagger swagger = MdwSwaggerCache.getSwagger("/");
            if (swagger != null) {
                for (String swaggerPath : swagger.getPaths().keySet()) {
                    inboundPaths.add(new ServicePath(swaggerPath));
                }
                Collections.sort(inboundPaths);
            }

            // outbound paths
            try {
                Map<String,List<AssetInfo>> swaggers = ServiceLocator.getAssetServices().findAssets(file ->
                    file.getName().equals("swagger.yaml") || file.getName().equals("swagger.json")
                );
                for (String pkg : swaggers.keySet()) {
                    for (AssetInfo swaggerAsset : swaggers.get(pkg)) {
                        if (swaggerAsset.getExtension().equals("yaml")) {
                            YamlLoader yamlLoader = new YamlLoader(swaggerAsset.getFile());
                            Map yamlPaths = yamlLoader.getMap("paths", yamlLoader.getTop());
                            if (yamlPaths != null) {
                                String basePath = yamlLoader.get("basePath", (Map)yamlLoader.getTop());
                                final String base = basePath == null ? "" : basePath;
                                yamlPaths.keySet().stream().forEach(p ->
                                    outboundPaths.add(new ServicePath(base + p.toString()))
                                );
                            }
                        }
                        else if (swaggerAsset.getExtension().equals("json")) {
                            String content = new String(Files.readAllBytes(swaggerAsset.getFile().toPath()));
                            JSONObject json = new JSONObject(content);
                            if (json.has("paths")) {
                                String base = json.optString("basePath");
                                JSONObject pathsJson = json.getJSONObject("paths");
                                for (String path : JSONObject.getNames(pathsJson)) {
                                    outboundPaths.add(new ServicePath(base + path));
                                }
                            }
                        }
                    }
                }
            }
            catch (ServiceException | IOException ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
        else {
            logger.info("ServicePaths cache disabled");
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
        ServicePath servicePath;
        try {
            servicePath = new ServicePath(new URL(url).getPath(), method);
        }
        catch (MalformedURLException ex) {
            servicePath = new ServicePath(url, method);
        }
        return servicePath.normalize(outboundPaths).toString();
    }
}
