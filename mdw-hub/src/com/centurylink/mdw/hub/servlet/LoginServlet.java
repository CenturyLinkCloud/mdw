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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.auth.Authenticator;
import com.centurylink.mdw.auth.MdwSecurityException;
import com.centurylink.mdw.auth.OAuthAuthenticator;
import com.centurylink.mdw.hub.context.WebAppContext;
import com.centurylink.mdw.model.user.AuthenticatedUser;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

@WebServlet(urlPatterns={"/login", "/logout"}, loadOnStartup=1)
public class LoginServlet extends HttpServlet {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        request.getSession().removeAttribute("authenticatedUser");
        String authMethod = WebAppContext.getMdw().getAuthMethod();
        if ("ct".equals(authMethod))
            request.getRequestDispatcher("auth/ctLogin.html").forward(request, response);
        else if ("af".equals(authMethod))
            request.getRequestDispatcher("auth/afLogin.html").forward(request, response);
        else
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Unsupported authMethod: " + authMethod);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        String authMethod = WebAppContext.getMdw().getAuthMethod();
        if ("af".equals(authMethod)) {
            String user = request.getParameter("user");
            String password = request.getParameter("password");
            if (user != null && !user.isEmpty() && password != null && !password.isEmpty()) {
                try {
                    Authenticator authenticator = new OAuthAuthenticator(WebAppContext.getMdw().getAuthTokenLoc());
                    authenticator.authenticate(user, password);
                    logger.info("User logged in: " + user);
                    AuthenticatedUser authUser = ServiceLocator.getUserManager().loadUser(user);
                    if (authUser == null) {
                        if (!WebAppContext.getMdw().isAllowAnyAuthenticatedUser())
                            throw new MdwSecurityException("User not authorized: " + authUser);
                    }
                    request.getSession().setAttribute("authenticatedUser", authUser);
                    response.sendRedirect(WebAppContext.getMdw().getHubRoot());
                }
                catch (Exception ex) {
                    logger.severeException(ex.getMessage(), ex);
                    response.sendRedirect(WebAppContext.getMdw().getHubRoot() + "/login");
                }
            }
        }
        else {
            super.doPost(request, response);
        }
    }
}
