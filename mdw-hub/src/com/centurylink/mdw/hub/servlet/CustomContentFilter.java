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
