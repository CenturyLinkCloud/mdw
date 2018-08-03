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
package com.centurylink.mdw.boot.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

import com.centurylink.mdw.hub.context.ContextPaths;
import com.centurylink.mdw.hub.servlet.AccessFilter;

@WebFilter(urlPatterns={"/*"})
public class WebContentFilter  implements Filter {

    @Autowired
    private ContextPaths contextPaths;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String path = request.getServletPath();
        if (request.getPathInfo() != null)
            path += request.getPathInfo();
        if (contextPaths.isHubPath(path)) {
            if (request.getSession().getAttribute("authenticatedUser") == null) {
                new AccessFilter().doFilter(servletRequest, servletResponse, chain);
            }
            request.getRequestDispatcher("/hub" + path).forward(servletRequest, servletResponse);
        }
        else {
            chain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void destroy() {
    }
}
