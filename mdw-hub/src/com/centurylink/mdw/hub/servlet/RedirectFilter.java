/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.servlet;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles HTTP redirects based on the mdw.redirect cookie.
 * Current use case is when user requests a specific URL in AdminUI, but their JSON
 * AuthUser request is redirected to login.jsf by ClearTrust (See AccessControlFilter).
 * After authentication, instead of letting CT direct to the default home page, if
 * the mdw.redirect cookie is set then the browser is redirected to its value.
 */
public class RedirectFilter implements Filter {

    private static final String MDW_REDIRECT_COOKIE = "mdw.redirect";

    // otherwise looping can occur
    private List<String> redirectEligiblePaths;

    public void init(FilterConfig filterConfig) throws ServletException {
        redirectEligiblePaths = new ArrayList<String>();
        redirectEligiblePaths.add("/Services/AuthenticatedUser");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            if (redirectEligiblePaths.contains(httpRequest.getServletPath() + httpRequest.getPathInfo())) {
                Cookie[] cookies = httpRequest.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if (MDW_REDIRECT_COOKIE.equals(cookie.getName())) {
                            String url = URLDecoder.decode(cookie.getValue(), "UTF-8");
                            HttpServletResponse httpResponse = (HttpServletResponse) response;
                            Cookie expCookie = new Cookie(MDW_REDIRECT_COOKIE, "");
                            expCookie.setMaxAge(0);
                            expCookie.setPath("/");
                            httpResponse.addCookie(expCookie);
                            httpResponse.sendRedirect(url);
                            return;
                        }
                    }
                }
            }
        }

        chain.doFilter(request, response);
    }

    public void destroy() {
    }
}
