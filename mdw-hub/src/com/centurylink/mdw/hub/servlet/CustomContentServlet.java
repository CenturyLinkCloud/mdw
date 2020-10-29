package com.centurylink.mdw.hub.servlet;

import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.hub.context.Mdw;
import com.centurylink.mdw.hub.context.WebAppContext;
import com.centurylink.mdw.model.asset.ContentTypes;
import com.centurylink.mdw.model.asset.api.AssetInfo;
import com.centurylink.mdw.util.ExpressionUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Serves up custom content from user-override mdw-hub package.
 */
@WebServlet(urlPatterns={"/customContent/*"}, loadOnStartup=1)
public class CustomContentServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getPathInfo();
        Mdw mdw = WebAppContext.getMdw();
        File file = mdw.getHubOverride(path);
        AssetInfo asset = new AssetInfo(file.getName(), file, 0, "0");
        String contentType = ContentTypes.getContentType(file);
        if (contentType == null) {
            // avoid firefox xml parsing errors
            if (file.getName().endsWith(".html"))
                contentType = "text/html";
            else if (file.getName().endsWith(".json"))
                contentType = "application/json";
            else if (file.getName().endsWith(".js"))
                contentType = "application/javascript";
            else if (file.getName().endsWith(".css"))
                contentType = "text/css";
        }
        if (contentType != null)
            response.setContentType(contentType);

        if (path.equals("/index.html")) {
            try {
                String html = new String(Files.readAllBytes(
                        Paths.get(mdw.getOverrideRoot() + "/index.html")));
                response.getOutputStream().print(ExpressionUtil.substitute(html, mdw, true));
            }
            catch (MdwException ex) {
                ex.printStackTrace();
            }
        }
        else {
            if (asset.shouldCache(request.getHeader("If-None-Match"))) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            }
            else {
                response.setHeader("ETag", asset.getETag());
                InputStream in = null;
                OutputStream out = response.getOutputStream();
                try {
                    in = new FileInputStream(file);
                    int read = 0;
                    byte[] bytes = new byte[1024];
                    while((read = in.read(bytes)) != -1)
                        out.write(bytes, 0, read);
                }
                finally {
                    if (in != null)
                        in.close();
                }
            }
        }
    }
}
