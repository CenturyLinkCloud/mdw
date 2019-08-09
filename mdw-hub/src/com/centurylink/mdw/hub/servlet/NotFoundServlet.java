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

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.hub.context.Mdw;
import com.centurylink.mdw.hub.context.Page;
import com.centurylink.mdw.hub.context.WebAppContext;
import com.centurylink.mdw.util.ExpressionUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Overrides 404 handling to check for matching asset content.
 * TODO: Only UI page requests should be handled here.  JS and CSS
 * requests should continue to return 404 status to browser/caller.
 */
@WebServlet(urlPatterns={"/404"}, loadOnStartup=1)
public class NotFoundServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String assetPath = request.getParameter("missingAsset");
        if (assetPath != null) {
            serveMissingAssetPage(request, response, assetPath);
        }
        else {
            String path = (String) request.getAttribute("javax.servlet.forward.servlet_path");
            if (path != null) {
                if (path.endsWith("/images/tab_sel.png")) {  // hack for nav back from Task UI
                    response.sendRedirect(ApplicationContext.getMdwHubUrl() + "/images/tab_sel.png");
                    return;
                }
                if (path.endsWith("/mdw.ico")) {  // hack for nav back from Task UI
                    response.sendRedirect(ApplicationContext.getMdwHubUrl() + "/images/mdw.ico");
                    return;
                }
                if (path.startsWith("/staging") || path.startsWith("/milestones")
                        || (path.indexOf('.') == -1 && path.indexOf('#') == -1 && path.startsWith("/tasks"))) {
                    String redirectPath = path;
                    String[] pathSegs = path.substring(1).split("/");
                    if (pathSegs.length > 2)
                        redirectPath = "/" + pathSegs[0] + "/" + pathSegs[1];
                    response.sendRedirect(ApplicationContext.getMdwHubUrl() + "/#" + redirectPath);
                    return;
                }
                if (path.startsWith("/edit/")) {
                    // TODO implement asset editing in React
                    response.sendRedirect(ApplicationContext.getMdwHubUrl() + "/#" + path);
                    return;
                }

                if (path.startsWith("/dashboard/")) {
                    // shortcut for standalone dashboard app
                    path = "/com/centurylink/mdw/dashboard/Index";
                }

                Mdw mdw = WebAppContext.getMdw();
                Page page = findPage(mdw, path);
                if (!page.exists()) {
                    String rootPkg = PropertyManager.getProperty(PropertyNames.MDW_HUB_ROOT_PACKAGE);
                    if (rootPkg != null)
                        page = findPage(mdw, "/" + rootPkg.replace('\\', '/') + path);
                }

                if (page.exists()) {
                    response.setContentType("text/html");

                    if (page.getAsset().shouldCache(request.getHeader("If-None-Match"))) {
                        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    } else {
                        response.setHeader("ETag", page.getAsset().getETag());
                        InputStream in = null;
                        OutputStream out = response.getOutputStream();
                        try {
                            if (page.getExt().equals("md")) {
                                // TODO: render markdown to html
                            } else if (page.getTemplate() != null) {
                                String html = new String(Files.readAllBytes(Paths.get(page.getTemplateAsset().getFile().getPath())));
                                in = new ByteArrayInputStream(ExpressionUtil.substitute(html, page, true).getBytes());
                            } else {
                                in = new FileInputStream(page.getFile());
                            }
                            int read;
                            byte[] bytes = new byte[1024];
                            if (in != null)
                                while ((read = in.read(bytes)) != -1)
                                    out.write(bytes, 0, read);
                        } catch (MdwException ex) {
                            throw new IOException(ex.getMessage(), ex);
                        } finally {
                            if (in != null)
                                in.close();
                            response.setStatus(HttpServletResponse.SC_OK);
                        }
                    }
                    return;
                }
                else if (path.equals("/com/centurylink/mdw/dashboard/Index")) {
                    String unfoundAsset = path.substring(1, path.lastIndexOf('/')).replace('/', '.');
                    String redirect = "/404?missingAsset=" + URLEncoder.encode(unfoundAsset, "utf-8");
                    request.getRequestDispatcher(redirect).forward(request, response);
                    return;
                }
            }
            request.getRequestDispatcher("/error/404.html").forward(request, response);
        }
    }

    private Page findPage(Mdw mdw, String path) {
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
                        Page ancestor = new Page(mdw, path).findAncestor("Index.jsx");
                        if (ancestor != null)
                            page = ancestor;
                    }
                    if (page.exists()) {
                        // standalone jsx path (without extension): set template html
                        page.setTemplate("com/centurylink/mdw/react/index.html");
                    }
                }
            }
            if (!page.exists()) {
                // allow 404 override
                page = new Page(mdw, "/error/404.html");
            }
        }
        return page;
    }

    private void serveMissingAssetPage(HttpServletRequest request, HttpServletResponse response, String assetPath)
    throws IOException {
        File htmlFile;
        if (ApplicationContext.isSpringBoot()) {
            // pure spring boot
            htmlFile = new File(ApplicationContext.getDeployPath() + "/hub/error/noAsset.html");
            if (!htmlFile.exists())
                htmlFile = new File(ApplicationContext.getDeployPath() + "/web/error/noAsset.html");
        }
        else {
            htmlFile = new File(request.getServletContext().getRealPath("error/noAsset.html"));
        }
        String html = new String(Files.readAllBytes(htmlFile.toPath()));
        Map<String,String> data = new HashMap<>();
        data.put("assetPath", assetPath);
        int slash = assetPath.indexOf('/');
        String packageName = slash > 0 ? assetPath.substring(0, assetPath.indexOf('/')) : assetPath;
        data.put("packageName", packageName);
        data.put("packagePath", packageName.replace('.', '/'));
        data.put("contextRoot", ApplicationContext.getMdwHubContextRoot());
        try {
            html = ExpressionUtil.substitute(html, data, true).replace('\n', ' ');
            response.setContentType("text/html");
            response.getWriter().println(html);
        }
        catch (MdwException ex) {
            throw new IOException(ex);
        }
    }
}
