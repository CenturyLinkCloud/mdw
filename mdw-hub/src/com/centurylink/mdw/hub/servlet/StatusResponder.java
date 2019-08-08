package com.centurylink.mdw.hub.servlet;

import com.centurylink.mdw.model.StatusResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class StatusResponder {

    private HttpServletResponse servletResponse;

    public StatusResponder(HttpServletResponse servletResponse) {
        this.servletResponse = servletResponse;
    }

    public void writeResponse(StatusResponse response) throws IOException {
        servletResponse.setStatus(response.getStatus().getCode());
        servletResponse.getWriter().println(response.getJson().toString(2));
    }
}
