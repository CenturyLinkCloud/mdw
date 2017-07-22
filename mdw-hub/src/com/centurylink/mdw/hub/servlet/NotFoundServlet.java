/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.hub.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.hub.context.Mdw;
import com.centurylink.mdw.hub.context.WebAppContext;
import com.centurylink.mdw.model.asset.AssetInfo;

/**
 * Overrides 404 handling to check for matching asset content.
 */
@WebServlet(urlPatterns={"/404"}, loadOnStartup=1)
public class NotFoundServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = (String)request.getAttribute("javax.servlet.forward.servlet_path");
        if (path != null) {
            Mdw mdw = WebAppContext.getMdw();
            AssetInfo asset = new AssetInfo(mdw.getAssetRoot(), path);
            if (!asset.exists() && asset.getExtension() == null) {
                // try appending page extensions
                // TODO: make this a method
                asset = new AssetInfo(mdw.getAssetRoot(), path + ".html");
                if (!asset.exists()) {
                    asset = new AssetInfo(mdw.getAssetRoot(), path + ".md");
                    if (!asset.exists()) {
                        asset = new AssetInfo(mdw.getAssetRoot(), path + ".jsx");
                    }
                }
                if (asset.exists()) {
                    path += asset.getExtension(); // append extension to the path
                }
                else {
                    path = "/error/404.html";
                    // allow 404 override
                    asset = new AssetInfo(mdw.getAssetRoot(), mdw.getOverridePackage() + path);
                }
            }
            if (asset.exists()) {
                if (asset.getExtension().equals("md")) {
                    // TODO: render markdown
                }
                else if (asset.getExtension().equals("jsx")) {
                    // TODO: forward to jsx handler
                    System.out.println("Requested JSX asset: " + asset);
                }
                else {
                    response.setContentType(asset.getContentType());

                    if (asset.shouldCache(request.getHeader("If-None-Match"))) {
                        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    }
                    else {
                        response.setHeader("ETag", asset.getETag());
                        String pkg = path.substring(0, path.length() - asset.getName().length());
                        File file = new File(mdw.getAssetRoot() + "/" + pkg.replace('.', '/') + "/" + asset.getName());
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
                            response.setStatus(HttpServletResponse.SC_OK);
                        }
                    }
                }
            }
            return;
        }

        request.getRequestDispatcher("/error/404.html").forward(request, response);
    }
}
