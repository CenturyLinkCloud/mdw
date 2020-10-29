package com.centurylink.mdw.boot.servlet;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.file.Files;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.centurylink.mdw.boot.MdwStarter;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.hub.context.ContextPaths;
import com.centurylink.mdw.hub.context.WebAppContext;

@WebServlet(urlPatterns = { "/hub/*" }, loadOnStartup = 1)
public class WebContentServlet  extends HttpServlet {

    @Autowired
    private MdwStarter mdwStarter;
    @Autowired
    private ContextPaths contextPaths;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getPathInfo();

        // special handling for index
        if (path.equals("/") || path.equals("/index.html")) {
            response.setCharacterEncoding("UTF-8");
            if (new File(WebAppContext.getMdw().getOverrideRoot() + "/index.html").isFile()) {
                request.getRequestDispatcher("/customContent/index.html").forward(request, response);
            }
            else {
                // standard index.html -- read and process (never cached since asset injection may change)
                response.setContentType("text/html");
                File file = mdwStarter.getFile("/hub/index.html");
                try {
                    String contents = new String(Files.readAllBytes(file.toPath()));
                    response.getWriter().println(contextPaths.processIndex(contents));
                }
                catch (IOException | MdwException ex) {
                    ex.printStackTrace();
                    throw new IOException(ex.getMessage(), ex);
                }
            }
        }
        else {
            File file = mdwStarter.getFile("/hub" + path);
            if (file.exists()) {
                if (shouldCache(file, request.getHeader("If-None-Match"))) {
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                }
                else {
                    response.setContentType(URLConnection.guessContentTypeFromName(file.getName()));
                    response.setContentLength((int)file.length());
                    response.setHeader("ETag", String.valueOf(file.lastModified()));
                    try (OutputStream out = response.getOutputStream()) {
                        Files.copy(file.toPath(), out);
                        out.flush();
                    }
                }
            }
            else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    private boolean shouldCache(File file, String ifNoneMatchHeader) {
        if (ifNoneMatchHeader == null)
            return false; // no cache
        try {
            long clientTime = Long.parseLong(ifNoneMatchHeader);
            return clientTime >= file.lastModified();
        }
        catch (NumberFormatException ex) {
            return false;
        }
    }
}
