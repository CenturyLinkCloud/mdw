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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.hub.context.WebAppContext;

/**
 * Forwards requests to custom content servlet if override asset exists.
 */
@WebFilter(urlPatterns={"/*"})
public class CustomContentFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {
        if (WebAppContext.getMdw().getOverrideRoot() != null) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String path = httpRequest.getServletPath();
            // forward to override servlet
            if (new File(WebAppContext.getMdw().getOverrideRoot() + path).isFile()) {
                // if authUser is null, redirect to avoid bypassing AccessFilter
                if (httpRequest.getSession().getAttribute("authenticatedUser") == null) {
                    String redirect = "/" + ApplicationContext.getMdwHubContextRoot() + "/customContent" + path;
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
        else {
            chain.doFilter(request, response);
        }
    }

    public void destroy() {
    }

}
