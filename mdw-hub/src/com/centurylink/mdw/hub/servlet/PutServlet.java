/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PutServlet extends HttpServlet {

    private File rootDir;

    @Override
    public void init() throws ServletException {
        String loc = getServletConfig().getInitParameter("mdw.put.location");
        rootDir = new File(getServletContext().getRealPath("/") + (loc == null ? "" : loc));
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!rootDir.isDirectory()) {
            response.sendError(HttpServletResponse.SC_CONFLICT, "Cannot find root directory: " + rootDir);
            return;
        }

        File file = new File(rootDir + request.getPathInfo());
        if (file.exists() && !"true".equalsIgnoreCase(getServletConfig().getInitParameter("mdw.put.overwrite"))) {
            response.sendError(HttpServletResponse.SC_CONFLICT, "Target already exists: " + file);
            return;
        }
        File dir = file.getParentFile();
        if (!dir.isDirectory() && !dir.mkdirs()) {
            response.sendError(HttpServletResponse.SC_CONFLICT, "Cannot create directory: " + dir);
            return;
        }

        OutputStream out = null;
        InputStream in = request.getInputStream();
        try {
            out = new FileOutputStream(file);
            int read = 0;
            byte[] bytes = new byte[1024];
            while((read = in.read(bytes)) != -1)
                out.write(bytes, 0, read);
        }
        finally {
            if (out != null)
                out.close();
        }
    }
}
