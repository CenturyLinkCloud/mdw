package com.centurylink.mdw.hub.servlet.asset;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.asset.Renderer;
import com.centurylink.mdw.services.asset.RenderingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class AssetRenderer {

    private HttpServletRequest servletRequest;
    private HttpServletResponse servletResponse;

    public AssetRenderer(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
    }

    public void renderAsset(String render, String path, String stagingCuid) throws IOException, ServiceException {
        AssetServices assetServices;
        if (stagingCuid != null) {
            assetServices = ServiceLocator.getStagingServices().getAssetServices(stagingCuid);
        }
        else {
            assetServices = ServiceLocator.getAssetServices();
        }
        Renderer renderer = assetServices.getRenderer(path, render.toUpperCase());
        if (renderer == null)
            throw new RenderingException(ServiceException.NOT_FOUND, "Renderer not found: " + render);
        String contentType = Asset.getContentType(render.toUpperCase());
        if (contentType != null)
            servletResponse.setContentType(contentType);
        Map<String,String> options = new HashMap<>();
        Enumeration<String> paramNames = servletRequest.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            options.put(paramName, servletRequest.getParameter(paramName));
        }
        servletResponse.getOutputStream().write(renderer.render(options));
    }
}
