/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.servlet;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.common.MDWException;
import com.centurylink.mdw.hub.context.WebAppContext;
import com.centurylink.mdw.util.ExpressionUtil;
import com.centurylink.mdw.util.file.FileHelper;

/**
 * Handles user-custom scripts and stylesheets by injecting into index.html.
 * Also substitutes ${mdw.*} expressions based on the Mdw model object.
 * Also handles the case where index.html itself is customized.
 * TODO: some kind of caching that still provides dynamicism
 */
public class RootServlet extends HttpServlet {

    private static final String MDW_ADMIN_JS = "<script src=\"js/admin.js\"></script>";
    private static final String MDW_ADMIN_CSS = "<link rel=\"stylesheet\" href=\"css/mdw-admin.css\">";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String path = request.getServletPath();
        if (path.equals("/root") || path.equals("/index.html")) {
            if (new File(WebAppContext.getMdw().getOverrideRoot() + "/index.html").isFile()) {
                request.getRequestDispatcher("/customContent/index.html").forward(request, response);
            }
            else {
                // standard index.html -- read and process (never cached since asset injection may change)
                response.setContentType("text/html");
                String realPath = request.getSession().getServletContext().getRealPath("/index.html");
                try {
                    String contents;
                    if (realPath == null)  // read from classpath
                        contents = new String(FileHelper.readFromResourceStream(getClass().getClassLoader().getResourceAsStream("index.html")));
                    else
                        contents = FileHelper.readFromFile(realPath);

                    contents = ExpressionUtil.substitute(contents, WebAppContext.getMdw(), true);
                    response.getWriter().println("<!-- processed by MDW root servlet -->");
                    for (String line : contents.split("\\r?\\n"))
                        response.getWriter().println(processLine(line));
                }
                catch (IOException ex) {
                    // TODO: logging
                    ex.printStackTrace();
                    throw ex;
                }
                catch (MDWException ex) {
                    // TODO: logging
                    ex.printStackTrace();
                    throw new IOException(ex.getMessage(), ex);
                }
            }
        }
    }

    /**
     * Inserts custom CSS and JS files.
     * TODO: redundantly re-adds override files (not harmful but ugly)
     */
    private String processLine(String line) throws IOException {
        if (line.trim().equals(MDW_ADMIN_CSS)) {
            // insert custom user stylesheets
            String indent = line.substring(0, line.indexOf(MDW_ADMIN_CSS));
            StringBuffer insert = new StringBuffer(line);
            for (File cssFile : WebAppContext.listOverrideFiles("css")) {
                insert.append("\n").append(indent);
                insert.append("<link rel=\"stylesheet\" href=\"css/");
                insert.append(cssFile.getName());
                insert.append("\">");
            }
            return insert.toString();
        }
        else if (line.trim().equals(MDW_ADMIN_JS)) {
            // insert custom user scripts
            String indent = line.substring(0, line.indexOf(MDW_ADMIN_JS));
            StringBuffer insert = new StringBuffer(line);
            for (File jsFile : WebAppContext.listOverrideFiles("js")) {
                insert.append("\n").append(indent);
                insert.append("<script src=\"js/");
                insert.append(jsFile.getName());
                insert.append("\"></script>");
            }
            return insert.toString();
        }
        else {
            return line;
        }
    }

}
