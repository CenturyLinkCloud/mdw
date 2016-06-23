/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.servlet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.utilities.ExpressionUtil;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;

public class JnlpLaunchServlet extends HttpServlet {
    private static final String DEFAULT_CODEBASE_URL = "http://qtdenvmdt317.dev.qintra.com:8101/RcpWebStart";
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private Map<String,String> jnlpTemplates = new HashMap<String,String>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String name = request.getServletPath().replaceAll("/", "").toLowerCase();
            String template = getJnlpTemplate(request, name);

            Map<String, String> params = new HashMap<String, String>();
            String jnlpUrl = request.getRequestURL().toString();
            if (!StringHelper.isEmpty(request.getQueryString()))
                jnlpUrl += "?" + request.getQueryString();
            params.put("jnlpUrl", jnlpUrl);
            params.put("codebaseUrl", getCodebaseUrl());

            String mdwHost = request.getParameter("mdw.host");
            if (mdwHost == null)
                mdwHost = ApplicationContext.getServerHost();
            params.put("mdwHost", mdwHost);

            String mdwPort = request.getParameter("mdw.port");
            if (mdwPort == null)
                mdwPort = String.valueOf(ApplicationContext.getServerPort());
            params.put("mdwPort", mdwPort);

            String contextRoot = request.getParameter("mdw.context.root");
            if (contextRoot == null)
                contextRoot = getContextRoot();
            if (contextRoot == null)
                contextRoot = "null";
            params.put("mdwContextRoot", contextRoot);

            String preselectType = request.getParameter("mdw.preselect.type");
            params.put("preSelectType", preselectType);

            String preselectId = request.getParameter("mdw.preselect.id");
            params.put("preSelectId", preselectId);

            String jnlp = ExpressionUtil.substitute(template, params);

            response.setContentType("application/x-java-jnlp-file");
            response.getWriter().print(jnlp);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ServletException("Unable to locate JNLP content. See server log for details.", ex);
        }
    }

    private String getJnlpTemplate(HttpServletRequest request, String name) {
        String template = jnlpTemplates.get(name);
        if (template == null) {
            String url = "http://" + request.getLocalName() + ":" + request.getLocalPort() + request.getContextPath() + "/" + name + ".jnlp";
            try {
                HttpHelper httpHelper = new HttpHelper(new URL(url));
                template = httpHelper.get();
                jnlpTemplates.put(name, template);
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
        return template;
    }

    private String getCodebaseUrl() {
        String codebaseUrl = DEFAULT_CODEBASE_URL;
        String rcpWebStartUrl = null;
        try {
            rcpWebStartUrl = PropertyManager.getProperty("MDWFramework.MDWDesigner/rcp.webstart.url");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        if (rcpWebStartUrl != null) {
            codebaseUrl = rcpWebStartUrl;
        }
        return codebaseUrl;
    }

    private String getContextRoot() {
        String contextRoot = null;
        try
        {
          URL servicesUrl = new URL(ApplicationContext.getServicesUrl());
          contextRoot = servicesUrl.getPath().replaceAll("/", "");
        }
        catch (MalformedURLException ex)
        {
          // ignore since only host and port are strictly required
        }
        return contextRoot;
    }

}
