/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
@WebServlet(urlPatterns={"/error"}, loadOnStartup=1)
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
