/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TestingServices;

/**
 * Serves up raw test cases and related resources.
 */
public class TestCaseServlet extends AssetContentServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

        if ("/testResult".equals(request.getServletPath())) {
            TestingServices testingServices = ServiceLocator.getTestingServices();
            File resultsDir = testingServices.getTestResultsDir();

            InputStream in = null;
            OutputStream out = response.getOutputStream();
            try {
                File file = new File(resultsDir + request.getPathInfo());
                if (!file.isFile()) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                response.setContentType("text/plain");
                in = new FileInputStream(file);
                int read = 0;
                byte[] bytes = new byte[1024];
                while ((read = in.read(bytes)) != -1)
                    out.write(bytes, 0, read);
            }
            finally {
                if (in != null)
                    in.close();
            }

        }
        else {
            super.doGet(request, response);
        }

    }
}
