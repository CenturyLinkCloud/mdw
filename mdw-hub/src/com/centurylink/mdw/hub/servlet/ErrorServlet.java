/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.auth.MdwSecurityException;

/**
 * Error must already have been logged elsewhere.
 */
public class ErrorServlet extends HttpServlet {

    protected void service(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        Object error = request.getAttribute("error");
        if (error instanceof MdwSecurityException) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, ((MdwSecurityException)error).getMessage());
        }
        else if (error != null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error.toString());
        }
        else {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unknown error");
        }
    }

}
