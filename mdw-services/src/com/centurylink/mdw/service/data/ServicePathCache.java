package com.centurylink.mdw.service.data;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.dataaccess.DbAccess;
import com.centurylink.mdw.model.request.ServicePath;
import com.centurylink.mdw.service.api.MdwSwaggerCache;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import io.swagger.models.Swagger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Caches normalized request paths.  These are stored in response
 * documents (whereas full paths are stored in request documents).
 */
public class ServicePathCache implements PreloadableCache {

    private static final int MAX_SIZE = 1000;

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static List<String> inboundPaths;
    private static List<String> outboundPaths;
    private static List<ServicePath> swaggerPaths;

    @Override
    public void initialize(Map<String,String> params) {
        // TODO defined path patterns (especially for adapters)
    }

    @Override
    public void loadCache() throws CachingException {
        load();
    }

    @Override
    public void refreshCache() {
        load();
    }

    @Override
    public void clearCache() {
    }

    private synchronized void load() {
        inboundPaths = new CopyOnWriteArrayList<>();
        outboundPaths = new CopyOnWriteArrayList<>();

        // default is enabled if dashboard package present
        boolean hasDashboard = PackageCache.getPackage("com.centurylink.mdw.dashboard") != null;
        if (!PropertyManager.getBooleanProperty("mdw.service.path.cache", hasDashboard)) {
            logger.info("ServicePathCache disabled");
            return;
        }

        swaggerPaths = new ArrayList<>();
        Swagger swagger = MdwSwaggerCache.getSwagger("/");
        if (swagger != null) {
            for (String swaggerPath : swagger.getPaths().keySet()) {
                swaggerPaths.add(new ServicePath(swaggerPath));
            }
            Collections.sort(swaggerPaths);
        }

        try (DbAccess dbAccess = new DbAccess()) {
            long before = System.currentTimeMillis();

            // group by is much faster than select distinct on mysql
            String sql = "select path from DOCUMENT where owner_type = ? and path is not null group by path";
            ResultSet resultSet = dbAccess.runSelect(sql, "LISTENER_RESPONSE");
            while (resultSet.next()) {
                inboundPaths.add(resultSet.getString(1));
            }
            inboundPaths.sort(String::compareToIgnoreCase);

            resultSet = dbAccess.runSelect(sql, "ADAPTER_RESPONSE");
            while (resultSet.next()) {
                outboundPaths.add(resultSet.getString(1));
            }
            outboundPaths.sort(String::compareToIgnoreCase);

            logger.info("ServicePathCache.load(): " + (System.currentTimeMillis() - before) + " ms");
        }
        catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public static List<String> getInboundPaths() { return inboundPaths; }
    public static List<String> getOutboundPaths() { return outboundPaths; }
}
