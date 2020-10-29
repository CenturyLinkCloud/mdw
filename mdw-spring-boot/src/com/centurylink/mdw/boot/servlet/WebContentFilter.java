package com.centurylink.mdw.boot.servlet;

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

import org.springframework.beans.factory.annotation.Autowired;

import com.centurylink.mdw.hub.context.ContextPaths;
import com.centurylink.mdw.hub.context.WebAppContext;
import com.centurylink.mdw.hub.servlet.AccessFilter;

@WebFilter(urlPatterns={"/*"})
public class WebContentFilter  implements Filter {

    @Autowired
    private ContextPaths contextPaths;

    @Override
    public void init(FilterConfig filterConfig) {
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
                // let access filter redirect if not authenticated
                new AccessFilter().doFilter(servletRequest, servletResponse, new FilterChain() {
                    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
                            throws IOException, ServletException {
                        HttpServletRequest request = (HttpServletRequest) servletRequest;
                        String path = request.getServletPath();
                        if (request.getPathInfo() != null)
                            path += request.getPathInfo();
                        if (new File(WebAppContext.getMdw().getOverrideRoot() + path).isFile()) {
                            request.getRequestDispatcher("/customContent" + path).forward(servletRequest, servletResponse);
                        }
                        else {
                            request.getRequestDispatcher("/hub" + path).forward(servletRequest, servletResponse);
                        }
                    }
                });
            }
            else if (new File(WebAppContext.getMdw().getOverrideRoot() + path).isFile()) {
                request.getRequestDispatcher("/customContent" + path).forward(servletRequest, servletResponse);
            }
            else {
                request.getRequestDispatcher("/hub" + path).forward(servletRequest, servletResponse);
            }
        }
        else {
            chain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void destroy() {
    }
}
