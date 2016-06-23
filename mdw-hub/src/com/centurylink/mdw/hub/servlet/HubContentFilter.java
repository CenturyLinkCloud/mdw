/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
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
import javax.servlet.http.HttpServletRequest;

import com.centurylink.mdw.common.ApplicationContext;

public class HubContentFilter implements Filter {

    private File hubOverrideRoot;

    public void init(FilterConfig filterConfig) throws ServletException {
        hubOverrideRoot = ApplicationContext.getHubOverrideRoot();
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {

        if (hubOverrideRoot != null) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String path = httpRequest.getServletPath();
            if ("GET".equalsIgnoreCase(httpRequest.getMethod()) && ApplicationContext.getHubWelcomePath().equals(path + httpRequest.getPathInfo())) {
                String hubUrl = ApplicationContext.getMdwHubUrl();
                if (!hubUrl.endsWith("/"))
                    hubUrl += "/";
                if (hubUrl.equals(httpRequest.getHeader("Referer")))
                    path = "/index.html";  // allow welcome page override
            }
            if (new File(hubOverrideRoot + path).isFile()) // forward to override servlet
                request.getRequestDispatcher("/hubContent" + path).forward(request, response);
            else
                chain.doFilter(request, response);
        }
        else {
            chain.doFilter(request, response);
        }
    }

    public void destroy() {
        hubOverrideRoot = null;
    }

}
