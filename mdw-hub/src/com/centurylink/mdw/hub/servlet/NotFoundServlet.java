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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.hub.context.Mdw;
import com.centurylink.mdw.hub.context.Page;
import com.centurylink.mdw.hub.context.WebAppContext;
import com.centurylink.mdw.util.ExpressionUtil;

/**
 * Overrides 404 handling to check for matching asset content.
 */
@WebServlet(urlPatterns={"/404"}, loadOnStartup=1)
public class NotFoundServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = (String)request.getAttribute("javax.servlet.forward.servlet_path");
        if (path != null) {
            if (path.endsWith("/images/tab_sel.png")) {  // hack for nav back from Task UI
                response.sendRedirect("/" + ApplicationContext.getMdwHubContextRoot() + "/images/tab_sel.png");
                return;
            }
            if (path.endsWith("/mdw.ico")) {  // hack for nav back from Task UI
                response.sendRedirect("/" + ApplicationContext.getMdwHubContextRoot() + "/mdw.ico");
                return;
            }
            if (path.indexOf('.') == -1 && path.indexOf('#') == -1 && path.startsWith("/tasks")) {
                String redirectPath = path;
                String[] pathSegs = path.substring(1).split("/");
                if (pathSegs.length > 2)
                    redirectPath = "/" + pathSegs[0] + "/" + pathSegs[1];
                response.sendRedirect("/" + ApplicationContext.getMdwHubContextRoot() + "/#" + redirectPath);
                return;
            }

            Mdw mdw = WebAppContext.getMdw();
            Page page = new Page(mdw, path);

            if (!page.exists()) {
                if (page.getExt() == null) {
                    // try appending supported page extensions
                    page = new Page(mdw, path + ".html");
                    if (!page.exists()) {
                        page = new Page(mdw, path + ".md");
                    }
                    if (!page.exists()) {
                        String assetPath = path;
                        if (assetPath.endsWith("/"))
                            assetPath = assetPath.substring(0, assetPath.length() - 1);
                        int lastSlash = assetPath.lastIndexOf('/');
                        String pkgPath = assetPath.substring(0, lastSlash);
                        String name = assetPath.substring(lastSlash + 1);
                        page = new Page(mdw, pkgPath + "/" + name.substring(0, 1).toUpperCase() + name.substring(1) + ".jsx");
                        if (!page.exists()) {
                            page = new Page(mdw, path).findAncestor("Index.jsx");
                        }
                        if (page.exists()) {
                            // standalone jsx path (without extension): set template html
                            page.setTemplate("com/centurylink/mdw/react/index.html");
                        }
                    }
                }
                if (!page.exists()) {
                    // allow 404 override
                    page = new Page(mdw, path).findAncestor("404.html");
                }
            }
            if (page.exists()) {
                response.setContentType("text/html");

                if (page.getAsset().shouldCache(request.getHeader("If-None-Match"))) {
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                }
                else {
                    response.setHeader("ETag", page.getAsset().getETag());
                    InputStream in = null;
                    OutputStream out = response.getOutputStream();
                    try {
                        if (page.getExt().equals("md")) {
                            // TODO: render markdown to html
                        }
                        else if (page.getTemplate() != null) {
                            String html = new String(Files.readAllBytes(Paths.get(page.getTemplateAsset().getFile().getPath())));
                            in = new ByteArrayInputStream(ExpressionUtil.substitute(html, page, true).getBytes());
                        }
                        else {
                            in = new FileInputStream(page.getFile());
                        }
                        int read = 0;
                        byte[] bytes = new byte[1024];
                        while((read = in.read(bytes)) != -1)
                            out.write(bytes, 0, read);
                    }
                    catch (MdwException ex) {
                        throw new IOException(ex.getMessage(), ex);
                    }
                    finally {
                        if (in != null)
                            in.close();
                        response.setStatus(HttpServletResponse.SC_OK);
                    }
                }
                return;
            }
        }

        request.getRequestDispatcher("/error/404.html").forward(request, response);
    }

}
