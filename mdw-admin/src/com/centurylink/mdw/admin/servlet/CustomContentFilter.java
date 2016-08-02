/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.admin.servlet;

import java.io.File;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.centurylink.mdw.admin.common.WebAppContext;

/**
 * Forwards requests to custom content servlet if override asset exists.
 */
public class CustomContentFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {
        if (WebAppContext.getMdw().getOverrideRoot() != null) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String path = httpRequest.getServletPath();
            // forward to override servlet
            if (new File(WebAppContext.getMdw().getOverrideRoot() + path).isFile())
                request.getRequestDispatcher("/customContent" + path).forward(request, response);
            else
                chain.doFilter(request, response);
        }
        else {
            chain.doFilter(request, response);
        }
    }

    public void destroy() {
    }

}
