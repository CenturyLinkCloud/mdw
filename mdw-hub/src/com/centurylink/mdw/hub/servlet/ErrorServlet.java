package com.centurylink.mdw.hub.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.auth.MdwSecurityException;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.StatusResponse;

/**
 * Error must already have been logged elsewhere.
 */
@WebServlet(urlPatterns={"/500"}, loadOnStartup=1)
public class ErrorServlet extends HttpServlet {

    protected void service(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        Object error = request.getAttribute("error");
        if (error instanceof MdwSecurityException) {
            StatusResponse sr = new StatusResponse(Status.FORBIDDEN, ((MdwSecurityException)error).getMessage());
            response.setStatus(sr.getStatus().getCode());
            response.getWriter().println(sr.getJson().toString(2));
        }
        else if (error != null) {
            StatusResponse sr = new StatusResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error.toString());
            response.setStatus(sr.getStatus().getCode());
            response.getWriter().println(sr.getJson().toString(2));
        }
        else {
            Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
            if (statusCode != null) {
                if (statusCode == 404) {
                    request.getRequestDispatcher("/404").forward(request, response);
                }
                else {
                    StatusResponse sr = StatusResponse.forCode(statusCode);
                    response.setStatus(sr.getStatus().getCode());
                    response.getWriter().println(sr.getJson().toString(2));
                }
            }
            else {
                StatusResponse sr = new StatusResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unknown error");
                response.setStatus(sr.getStatus().getCode());
                response.getWriter().println(sr.getJson().toString(2));
            }
        }
    }

}
