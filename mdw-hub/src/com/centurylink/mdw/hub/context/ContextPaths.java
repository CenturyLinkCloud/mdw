/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.hub.context;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.util.ExpressionUtil;

public class ContextPaths {

    /**
     * Root (/) and /index.html are allowed to be handled by base hub.
     */
    private static final List<String> DEFAULT_NON_HUB_ROOTS = new ArrayList<>();
    static {
        DEFAULT_NON_HUB_ROOTS.add("/api");
        DEFAULT_NON_HUB_ROOTS.add("/api-docs");
        DEFAULT_NON_HUB_ROOTS.add("/services");
        DEFAULT_NON_HUB_ROOTS.add("/Services");
        DEFAULT_NON_HUB_ROOTS.add("/REST");
        DEFAULT_NON_HUB_ROOTS.add("/soap");
        DEFAULT_NON_HUB_ROOTS.add("/SOAP");
        DEFAULT_NON_HUB_ROOTS.add("/websocket");
        DEFAULT_NON_HUB_ROOTS.add("/asset");
        DEFAULT_NON_HUB_ROOTS.add("/attach");
        DEFAULT_NON_HUB_ROOTS.add("/customContent");
        DEFAULT_NON_HUB_ROOTS.add("/template");
        DEFAULT_NON_HUB_ROOTS.add("/testCase");
        DEFAULT_NON_HUB_ROOTS.add("/testResult");
        DEFAULT_NON_HUB_ROOTS.add("/login");
        DEFAULT_NON_HUB_ROOTS.add("/logout");
        DEFAULT_NON_HUB_ROOTS.add("/error");
        DEFAULT_NON_HUB_ROOTS.add("/404");
        DEFAULT_NON_HUB_ROOTS.add("*.jsx");
    }

    private static final String MDW_ADMIN_JS = "<script src=\"js/admin.js\"></script>";
    private static final String MDW_ADMIN_CSS = "<link rel=\"stylesheet\" href=\"css/mdw-admin.css\">";

    private static volatile List<String> nonHubRoots;

    /**
     * Root paths that aren't part of MDWHub UI.
     * Values that don't end with / are exact.
     * Values prefixed with * match by extension.
     * Customize in access.yaml.
     */
    public List<String> getNonHubRoots() throws IOException {
        List<String> nonHubRootsTemp = nonHubRoots;
        if (nonHubRootsTemp == null) {
            synchronized(ContextPaths.class) {
                nonHubRootsTemp = nonHubRoots;
                if (nonHubRootsTemp == null) {
                    nonHubRoots = DEFAULT_NON_HUB_ROOTS;
                    List<String> customPaths = WebAppContext.getMdw().getCustomPaths();
                    if (customPaths != null) {
                        nonHubRoots.addAll(customPaths);
                    }
                }
            }
        }
        return nonHubRoots;
    }

    /**
     * Returns true unless excluded by getNonHubRoots().
     */
    public boolean isHubPath(String path) throws IOException {
        if ("/".equals(path))
            return true;
        for (String root : getNonHubRoots()) {
            if (root.startsWith("*")) {
                if (path.endsWith(root.substring(1)))
                    return false;
            }
            else if (path.equals(root) || path.startsWith(root + "/")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handles user-custom scripts and stylesheets by injecting into index.html.
     * Also substitutes ${mdw.*} expressions based on the Mdw model object.
     * Also handles the case where index.html itself is customized.
     * TODO: some kind of caching that still provides dynamicism
     */
    public String processIndex(String content) throws IOException, MdwException {
        String contents = ExpressionUtil.substitute(content, WebAppContext.getMdw(), true);
        StringBuilder builder = new StringBuilder();
        builder.append("<!-- processed by MDW root servlet -->\n");
        for (String line : contents.split("\\r?\\n")) {
            builder.append(processIndexLine(line)).append("\n");
        }
        return builder.toString();
    }

    /**
     * Inserts custom CSS and JS files.
     * TODO: redundantly re-adds override files (not harmful but ugly)
     */
    private String processIndexLine(String line) throws IOException {
        if (line.trim().equals(MDW_ADMIN_CSS)) {
            // insert custom user stylesheets
            String indent = line.substring(0, line.indexOf(MDW_ADMIN_CSS));
            StringBuffer insert = new StringBuffer(line);
            for (File cssFile : WebAppContext.listOverrideFiles("css")) {
                insert.append("\n").append(indent);
                insert.append("<link rel=\"stylesheet\" href=\"css/");
                insert.append(cssFile.getName());
                insert.append("\">");
            }
            return insert.toString();
        }
        else if (line.trim().equals(MDW_ADMIN_JS)) {
            // insert custom user scripts
            String indent = line.substring(0, line.indexOf(MDW_ADMIN_JS));
            StringBuffer insert = new StringBuffer(line);
            for (File jsFile : WebAppContext.listOverrideFiles("js")) {
                insert.append("\n").append(indent);
                insert.append("<script src=\"js/");
                insert.append(jsFile.getName());
                insert.append("\"></script>");
            }
            return insert.toString();
        }
        else {
            return line;
        }
    }
}
