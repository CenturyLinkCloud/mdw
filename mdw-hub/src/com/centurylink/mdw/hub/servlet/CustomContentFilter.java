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
import com.centurylink.mdw.hub.context.WebAppContext;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Forwards requests to custom content servlet if override asset exists.
 */
@WebFilter(urlPatterns={"/*"})
public class CustomContentFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getServletPath();
        if (WebAppContext.getMdw().getHubOverride(path) != null) {
            // forward to override servlet
            // however, if authUser is null redirect to avoid bypassing AccessFilter
            if (httpRequest.getSession().getAttribute("authenticatedUser") == null) {
                String hubRoot = ApplicationContext.getMdwHubContextRoot();
                if (!hubRoot.isEmpty())
                    hubRoot = "/" + hubRoot;
                String redirect = hubRoot + "/customContent" + path;
                if (httpRequest.getQueryString() != null)
                    redirect += "?" + httpRequest.getQueryString();
                ((HttpServletResponse)response).sendRedirect(redirect);
            }
            else {
                request.getRequestDispatcher("/customContent" + path).forward(request, response);
            }
        }
        else {
            chain.doFilter(request, response);
        }
    }

    public void destroy() {
    }

}
