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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.auth.AuthenticationException;
import com.centurylink.mdw.auth.Authenticator;
import com.centurylink.mdw.auth.MdwAuthenticator;
import com.centurylink.mdw.auth.MdwSecurityException;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.hub.context.WebAppContext;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.StatusResponse;
import com.centurylink.mdw.model.user.AuthenticatedUser;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.ExpressionUtil;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

@WebServlet(urlPatterns = { "/login", "/logout" }, loadOnStartup = 1)
public class LoginServlet extends HttpServlet {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static final String MDW_MSG_TAG = "<div id=\"mdwAuthError\"></div>";
    private static final String AUTHENTICATION_FAILED_MSG = "Invalid user/password";
    private static final String AUTHORIZATION_FAILED_MSG = "User not authorized";
    private static final String SECURITY_ERR_MSG = "Security Exception occured";
    private static final String MDW_AUTH_MSG = "mdwAuthError";

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getSession().removeAttribute("authenticatedUser");
        String authError = (String) request.getSession().getAttribute(MDW_AUTH_MSG);
        String authMethod = WebAppContext.getMdw().getAuthMethod();
        if ("ct".equals(authMethod) || ("mdw".equals(authMethod) && "mdw-central".equals(ApplicationContext.getAppId()))) {
            if (request.getServletPath().equalsIgnoreCase("/logout")) {
                response.sendRedirect(ApplicationContext.getMdwHubUrl() + "/login");
                return;
            }
            else {
                response.setContentType("text/html");
                request.getSession().removeAttribute(MDW_AUTH_MSG);
                String contents = readContent(request, authMethod);
                response.getWriter().println("<!-- processed by MDW Login servlet -->");
                if (authError != null) {
                    for (String line : contents.split("\\r?\\n")) {
                        String retLine = processLine(line, authError);
                        response.getWriter().println(retLine);
                    }
                }
                else {
                    response.getWriter().print(contents);
                }
                response.getWriter().close();
            }
        }
        else if ("mdw".equals(authMethod)) {  // MDW Auth - Redirect to mdw-central to log in
            response.sendRedirect(ApplicationContext.getMdwCentralUrl() + "/signin?returnURL=" + ApplicationContext.getMdwHubUrl() + "/&appID=" + ApplicationContext.getAppId());
        }
        else {
            StatusResponse sr = new StatusResponse(Status.METHOD_NOT_ALLOWED,
                    "Unsupported authMethod: " + authMethod);
            response.setStatus(sr.getStatus().getCode());
            response.getWriter().println(sr.getJson().toString(2));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String authMethod = WebAppContext.getMdw().getAuthMethod();
        if ("mdw".equals(authMethod)) {
            String user = request.getParameter("user");
            String password = request.getParameter("password");
            if (user != null && !user.isEmpty() && password != null && !password.isEmpty()) {
                try {
                    String appId = ApplicationContext.getAppId();
                    if (appId == null)
                        throw new PropertyException("Missing property: mdw.app.id");
                    Authenticator authenticator = new MdwAuthenticator(appId);
                    authenticator.authenticate(user, password);
                    logger.info("User logged in: " + user);
                    AuthenticatedUser authUser = null;
                    User u = ServiceLocator.getUserServices().getUser(user);
                    if (u != null)
                      authUser = new AuthenticatedUser(u, u.getAttributes());
                    if (authUser == null) {
                        if (!WebAppContext.getMdw().isAllowAnyAuthenticatedUser()) {
                            throw new MdwSecurityException((Status.UNAUTHORIZED).getCode(), AUTHORIZATION_FAILED_MSG);
                        }
                    }
                    else {
                        request.getSession().setAttribute("authenticatedUser", authUser);
                        response.sendRedirect(ApplicationContext.getMdwHubUrl() + "/");
                    }

                }
                catch (Exception ex) {
                    logger.severeException(ex.getMessage(), ex);
                    if (ex instanceof AuthenticationException) {
                        request.getSession().setAttribute(MDW_AUTH_MSG, AUTHENTICATION_FAILED_MSG);
                    }
                    else if (ex instanceof MdwSecurityException
                            && ((MdwSecurityException) ex).getCode() == 401) {
                        request.getSession().setAttribute(MDW_AUTH_MSG, AUTHORIZATION_FAILED_MSG);
                    }
                    else {
                        request.getSession().setAttribute(MDW_AUTH_MSG, SECURITY_ERR_MSG);
                    }
                    response.sendRedirect(ApplicationContext.getMdwHubUrl() + "/login");
                }
            }
            else {
                request.getSession().setAttribute(MDW_AUTH_MSG, AUTHENTICATION_FAILED_MSG);
                response.sendRedirect(ApplicationContext.getMdwHubUrl() + "/login");
            }
        }
        else {
            super.doPost(request, response);
        }
    }

    private String readContent(HttpServletRequest request,String authMethod) throws IOException {
        String realPath = request.getSession().getServletContext().getRealPath("auth/" + authMethod + "Login.html");
        String contents;
        try {
            if (realPath == null) {
                // read from classpath
                contents = new String(FileHelper.readFromResourceStream(
                        getClass().getClassLoader().getResourceAsStream("auth/" + authMethod + "Login.html")));
            }
            else {
                if (!new File(realPath).isFile() && getClass().getClassLoader().getResource(
                        "/org/springframework/web/servlet/ResourceServlet.class") != null) {
                    // spring-boot client app
                    try (Scanner scanner = new Scanner(getClass().getClassLoader()
                            .getResourceAsStream("mdw/auth/" + authMethod + "Login.html"), "utf-8")) {
                        contents = scanner.useDelimiter("\\Z").next();
                    }
                }
                else if (new File(WebAppContext.getMdw().getOverrideRoot() + "/login.html").isFile()) {
                    String html = new String(Files.readAllBytes(
                            Paths.get(WebAppContext.getMdw().getOverrideRoot() + "/login.html")));
                    contents = ExpressionUtil.substitute(html, WebAppContext.getMdw(), true);
                }
                else {
                    contents = FileHelper.readFromFile(realPath);
                }
            }
        }
        catch (IOException ex) {
            // TODO: logging
            ex.printStackTrace();
            throw ex;
        }
        catch (MdwException ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage(), ex);
        }
        return contents;
    }

    private String processLine(String line, String replacementText) throws IOException {
        String newLine;
        if (line.trim().equals(MDW_MSG_TAG)) {
            newLine = "<div id=\"mdwAuthError\" class=\"mdw_hubMessage\">" + replacementText + "</div>";
            return newLine;
        }
        else {
            return line;
        }
    }
}
