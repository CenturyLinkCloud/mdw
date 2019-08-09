package com.centurylink.mdw.hub.servlet.asset;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.hub.servlet.StatusResponder;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.StatusResponse;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.StagingServices;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

public class StagedAssetServer {

    private static final StandardLogger logger = LoggerUtil.getStandardLogger();

    private HttpServletRequest servletRequest;
    private HttpServletResponse servletResponse;

    public StagedAssetServer(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
    }

    public void serveAsset(String stagingCuid, String path) throws IOException, ServiceException {

        StagingServices stagingServices = ServiceLocator.getStagingServices();
        File assetRoot = stagingServices.getStagingAssetsDir(stagingCuid);

        AssetInfo asset = new AssetInfo(assetRoot, path);
        File assetFile = asset.getFile();
        if (!assetFile.isFile()) {
            new StatusResponder(servletResponse).writeResponse(new StatusResponse(Status.NOT_FOUND));
            return;
        }

        String render = servletRequest.getParameter("render");
        if (render != null) {
            try {
                new AssetRenderer(servletRequest, servletResponse).renderAsset(render, path, stagingCuid);
            }
            catch (ServiceException ex) {
                logger.error(ex.getMessage(), ex);
                new StatusResponder(servletResponse).writeResponse(new StatusResponse(ex.getCode(), ex.getMessage()));
            }
            return;
        }

        boolean download = "true".equalsIgnoreCase(servletRequest.getParameter("download"));
        if (download) {
            servletResponse.setHeader("Content-Disposition", "attachment;filename=\"" + asset.getFile().getName() + "\"");
            servletResponse.setContentType("application/octet-stream");
        }
        else {
            servletResponse.setContentType(asset.getContentType());
        }

        OutputStream out = servletResponse.getOutputStream();
        try (InputStream in = new FileInputStream(assetFile)) {
            int read;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1)
                out.write(bytes, 0, read);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }
}
