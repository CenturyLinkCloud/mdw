/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.admin.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.admin.common.Mdw;
import com.centurylink.mdw.admin.common.WebAppContext;
import com.centurylink.mdw.model.value.asset.Asset;

/**
 * Serves up custom content from user-override mdw-admin package.
 */
public class CustomContentServlet extends HttpServlet {
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getPathInfo().substring(1);
        Mdw mdw = WebAppContext.getMdw();
        Asset asset = new Asset(mdw.getAssetRoot(), mdw.getOverridePackage() + "/" + path);
        response.setContentType(asset.getContentType());

        if (asset.shouldCache(request.getHeader("If-None-Match"))) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        }
        else {
            response.setHeader("ETag", asset.getETag());
            File file = new File(mdw.getAssetRoot() + "/" + mdw.getOverridePackage().replace('.', '/') + "/" + request.getPathInfo());
            InputStream in = null;
            OutputStream out = response.getOutputStream();
            try {
                in = new FileInputStream(file);
                int read = 0;
                byte[] bytes = new byte[1024];
                while((read = in.read(bytes)) != -1)
                    out.write(bytes, 0, read);
            }
            finally {
                if (in != null)
                    in.close();
            }
        }
    }
}
