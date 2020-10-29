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
