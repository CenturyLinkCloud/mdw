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

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.service.api.MdwSwagger;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import io.swagger.models.Swagger;
import io.swagger.util.Json;
import io.swagger.util.Yaml;

/**
 * Scans a service path for Swagger annotations and generates the service spec in JSON or YAML.
 */
@WebServlet(urlPatterns={"/api/*"}, loadOnStartup=1)
public class ServiceApiServlet extends HttpServlet {

    private static final String YAML_EXT = ".yaml";
    private static final String JSON_EXT = ".json";
    private static final String SWAGGER_PATH = "/Swagger";
    private static final String PRETTY_PRINT_PARAM = "prettyPrint";

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public void init(ServletConfig servletConfig) throws javax.servlet.ServletException {
        super.init(servletConfig);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // no extension == JSON_EXT
        String pathInfo = request.getPathInfo();
        if (pathInfo == null)
            pathInfo = SWAGGER_PATH + JSON_EXT;
        int dot = pathInfo.lastIndexOf(".");
        if (dot == -1) {
            dot = pathInfo.length();
            pathInfo += JSON_EXT;
        }
        String ext = pathInfo.substring(dot);
        if (!ext.equals(YAML_EXT) && !ext.equals(JSON_EXT))
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported extension: " + ext);
        else {
            String root = pathInfo.substring(0, dot);
            String svcPath;
            if (root.equals(SWAGGER_PATH) || root.equals(SWAGGER_PATH.toLowerCase()))
                svcPath = "/";
            else
                svcPath = root;

            svcPath = svcPath.replace('.', '/');

            try {
                boolean pretty = !"false".equals(request.getParameter(PRETTY_PRINT_PARAM));
                Swagger swagger = MdwSwagger.getSwagger(svcPath, pretty);

                if (ext.equals(JSON_EXT)) {
                    response.setContentType("application/json");
                    JsonObject swaggerJson = new JsonObject(Json.mapper().writeValueAsString(swagger));
                    // sort definitions and paths
                    if (swaggerJson.has("definitions"))
                        swaggerJson.put("definitions", new JsonObject(swaggerJson.getJSONObject("definitions").toString()));
                    if (swaggerJson.has("paths"))
                        swaggerJson.put("paths", new JsonObject(swaggerJson.getJSONObject("paths").toString()));
                    if (pretty)
                        response.getWriter().println(swaggerJson.toString(2));
                    else
                        response.getWriter().println(swaggerJson.toString());
                }
                else if (ext.equals(YAML_EXT)) {
                    response.setContentType("text/yaml");
                    response.getWriter().println(Yaml.mapper().writeValueAsString(swagger));
                }
            }
            catch (Exception ex) {
                String msg = "Swagger generation failure for : " + pathInfo;
                logger.severeException(msg, ex);
                response.sendError(501, msg);
            }
        }
    }
}
