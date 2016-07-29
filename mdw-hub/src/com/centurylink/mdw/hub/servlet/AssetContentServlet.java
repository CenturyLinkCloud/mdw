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
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.value.asset.Asset;

/**
 * Only works for VCS assets.
 */
public class AssetContentServlet extends HttpServlet {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

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
        boolean gitRemote = "true".equalsIgnoreCase(request.getParameter("gitRemote"));

        if (!asset.getFile().isFile() && !gitRemote) {
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
            if (gitRemote) {
                String branch = PropertyManager.getProperty(PropertyNames.MDW_GIT_BRANCH);
                if (branch == null)
                    throw new PropertyException("Missing required property: " + PropertyNames.MDW_GIT_BRANCH);
                VersionControlGit vcGit = VersionControlGit.getFrameworkGit();
                String gitPath = vcGit.getRelativePath(asset.getFile());
                in = vcGit.getRemoteContentStream(branch, gitPath);
                if (in == null)
                    throw new IOException("Git remote not found: " + gitPath);
            }
            else {
                if (!asset.getFile().isFile())
                    throw new IOException("Asset file not found: " + asset.getFile());
                in = new FileInputStream(asset.getFile());
            }
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1)
                out.write(bytes, 0, read);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        finally {
            if (in != null)
                in.close();
        }
    }
}
