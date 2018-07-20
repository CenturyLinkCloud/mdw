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

/**
 * Takes the place of welcome-files in web.xml
 *
 */
@WebFilter(urlPatterns={"/"})
public class RootFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
    throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)req;
        if (request.getServletPath().equals("/") && request.getPathInfo() == null) {
            // if authUser is null, redirect to avoid bypassing AccessFilter
            if (request.getSession().getAttribute("authenticatedUser") == null) {
                String redirect = "/" + ApplicationContext.getMdwHubContextRoot() + "/index.html";
                if (request.getQueryString() != null)
                    redirect += "?" + request.getQueryString();
                else if (request.getHeader("Authorization") != null && request.getHeader("Authorization").startsWith("Bearer ")) // ios
                    redirect += "?Authorization=" + request.getHeader("Authorization");
                ((HttpServletResponse)resp).sendRedirect(redirect);
            }
            else {
                request.getRequestDispatcher("/index.html").forward(req, resp);
            }
        }
        else {
            chain.doFilter(req, resp);
        }
    }

    public void destroy() {
    }
}
