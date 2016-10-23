/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class AccessControlFilter implements Filter {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
    throws IOException, ServletException {
        if (!(req instanceof HttpServletRequest))
            return;

        HttpServletRequest request = (HttpServletRequest) req;
        HttpSession session = request.getSession();

        if (session.isNew()) {
            logger.debug("-- new HTTP session from " + request.getRemoteHost());
            String timeoutProp = PropertyManager.getProperty(PropertyNames.MDW_WEB_SESSION_TIMEOUT);
            try {
                if (timeoutProp != null) {
                    session.setMaxInactiveInterval(Integer.parseInt(timeoutProp));
                }
                else {
                    int sessionTimeout = session.getMaxInactiveInterval();
                    if (sessionTimeout < 1800)
                        session.setMaxInactiveInterval(1800);
                }
            }
            catch (NumberFormatException e) {
                logger.severe("Invalid value ignored: " + PropertyNames.MDW_WEB_SESSION_TIMEOUT + "=" + timeoutProp);
            }
        }
    }

    public void destroy() {
    }

}