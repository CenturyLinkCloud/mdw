package com.centurylink.mdw.hub.servlet.asset;

import com.centurylink.mdw.cache.asset.AssetHistory;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Serves up previous asset versions.
 */
public class VersionedAssetServer {
    private static final StandardLogger logger = LoggerUtil.getStandardLogger();

    private HttpServletRequest servletRequest;
    private HttpServletResponse servletResponse;

    public VersionedAssetServer(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
    }

    /**
     * Old versions are retrieved from history.
     */
    public void serveAsset(String path, String version) throws IOException, ServiceException {
        Asset asset = AssetHistory.getAsset(path, version);
        if (asset == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Asset not found: " + path + " v" + version);
        if ("true".equalsIgnoreCase(servletRequest.getParameter("download"))) {
            String filename = path.substring(path.indexOf("/") + 1);
            servletResponse.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"");
            servletResponse.setContentType("application/octet-stream");
        }
        try {
            OutputStream out = servletResponse.getOutputStream();
            out.write(asset.getContent());
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
