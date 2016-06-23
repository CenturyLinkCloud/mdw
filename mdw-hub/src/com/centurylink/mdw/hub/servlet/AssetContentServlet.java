/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.model.value.asset.Asset;

/**
 * Only works for VCS assets.
 */
public class AssetContentServlet extends HttpServlet {

    private File assetRoot;

    public void init() throws ServletException {
        assetRoot = ApplicationContext.getAssetRoot();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        if (!assetRoot.isDirectory())
            throw new ServletException(assetRoot + " is not a directory");

        String path = request.getPathInfo().substring(1);
        Asset asset = new Asset(assetRoot, path);

        if (!asset.getFile().isFile()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if ("true".equalsIgnoreCase(request.getParameter("download"))) {
            response.setHeader("Content-Disposition", "attachment;filename=\"" + asset.getFile().getName() + "\"");
            response.setContentType("application/octet-stream");
        }
        else {
            response.setContentType(asset.getContentType());
        }

        InputStream in = null;
        OutputStream out = response.getOutputStream();

        try {
            in = new FileInputStream(asset.getFile());
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1)
                out.write(bytes, 0, read);
        }
        finally {
            if (in != null)
                in.close();
        }

    }
}
