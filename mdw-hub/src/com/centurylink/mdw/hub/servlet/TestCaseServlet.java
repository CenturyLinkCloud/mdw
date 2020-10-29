package com.centurylink.mdw.hub.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TestingServices;
import com.centurylink.mdw.test.TestCaseItem;

/**
 * Serves up raw test cases and related resources.
 */
@WebServlet(urlPatterns={"/testCase/*", "/testResult/*"}, loadOnStartup=1)
public class TestCaseServlet extends AssetContentServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

        TestCaseItem subItem = null;
        String path = request.getPathInfo().substring(1);
        String[] segments = path.split("/");
        if (segments.length == 3) {
            TestingServices testingServices = ServiceLocator.getTestingServices();
            try {
                subItem = testingServices.getTestCaseItem(path);
            }
            catch (ServiceException ex) {
                // todo log
                throw new IOException(ex.getMessage(), ex);
            }
        }

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
            if (subItem != null) {
                response.setContentType("application/json");
                response.getWriter().println(subItem.getObject().toString(2));
            }
            else {
                super.doGet(request, response);
            }
        }

    }
}
