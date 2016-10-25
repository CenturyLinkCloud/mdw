/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.hub.context.WebAppContext;

public class LoginServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        String authMethod = WebAppContext.getMdw().getAuthMethod();
        if ("ct".equals(authMethod))
            request.getRequestDispatcher("auth/ctLogin.html").forward(request, response);
        else
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Unsupported authMethod: " + authMethod);
    }
}
