/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.servlet;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.listener.http.FormServer;
import com.centurylink.mdw.listener.http.HTTPListenerHelper;

/**
 * The interface needs to be implemented if it is desired
 * that the class is loaded and executed when the server is started
 * The framework will load this class and execute the onStartup method
 * at start up.
 */

public class HTTPListenerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    /**
     * Method that container invokes when the servlet is initialized
     * @param pConfig
     * @throws ServletException
     */
    public void init(ServletConfig pConfig) throws ServletException {
    	logger.info("HTTPListenerServlet is initialized for "
    			+ pConfig.getServletContext().getServletContextName());
    	super.init(pConfig);
    }

   /**
    * Method that gets invoked
    * Called by the server (via the service method) to allow a servlet to handle a POST request.
    * @param pRequest
    * @param pResponse
    */
    @Override
    protected void doPost(HttpServletRequest pRequest, HttpServletResponse pResponse)
   		throws ServletException, IOException {
  	   String pathInfo = pRequest.getPathInfo();
    	if (pathInfo.equals("/form") || pathInfo.equals("/formuf")) {
    		FormServer handler = FormServer.getInstance();
    		handler.process_form(pRequest, pResponse);
    	} else if (pathInfo.equals("/json")) {
        	HTTPListenerHelper helper = new HTTPListenerHelper();
        	helper.handleJsonService(pRequest, pResponse);
        } else if (pathInfo.equals("/ajax")) {
        	FormServer handler = FormServer.getInstance();
        	handler.handleHtmlAjax(pRequest, pResponse);
        } else if (pathInfo.equals("/service") || pathInfo.equals("/REST")) {
        	// can be /listener/REST or /Services/REST when sent by designer and IntraMDWMessengerRest
        	HTTPListenerHelper helper = new HTTPListenerHelper();
        	helper.handleXmlService(pRequest, pResponse);
        } else if (pathInfo.equals("/login")) {
        	HTTPListenerHelper helper = new HTTPListenerHelper();
        	helper.doLogin(pRequest, pResponse);
        } else if (pathInfo.equals("/checkclass")) {
        	HTTPListenerHelper helper = new HTTPListenerHelper();
        	helper.checkClass(pRequest, pResponse, this.getClass());
        } else if (pathInfo.equals("/resource")){
        	HTTPListenerHelper helper = new HTTPListenerHelper();
        	helper.getResource(pRequest, pResponse);
        } else if (pRequest.getServletPath().equals("/Services")) {
            RequestDispatcher requestDispatcher = pRequest.getRequestDispatcher("/rest");
            requestDispatcher.forward(pRequest, pResponse);
        } else {
            throw new ServletException("Unknown post request " + pRequest.getRequestURI());
        }
    }

   /**
    * Method that gets invoked
    * Called by the server (via the service method) to allow a servlet to handle a POST request.
    * @param pRequest
    * @param pResponse
    */
   @Override
   protected void doGet(HttpServletRequest pRequest, HttpServletResponse pResponse)
   		throws ServletException, IOException {
	   String pathInfo = pRequest.getPathInfo();
	   if (pathInfo.equals("/form") || pathInfo.equals("/formuf")) {
		   FormServer handler = FormServer.getInstance();
		   handler.process_form(pRequest, pResponse);
	   } else if (pathInfo.equals("/resource")) {
		   HTTPListenerHelper helper = new HTTPListenerHelper();
		   helper.getResource(pRequest, pResponse);
	   } else if (pathInfo.startsWith("/resource/")){
		   // e.g. http://localhost:7001/MDWDesignerWeb/listener/resource/qwest.gif
		   HTTPListenerHelper helper = new HTTPListenerHelper();
		   String refresh = pRequest.getParameter("refresh");
		   helper.getResource(pathInfo, "true".equalsIgnoreCase(refresh), pResponse);
	   } else if (pathInfo.equals("/ajax")) {
		   FormServer handler = FormServer.getInstance();
		   handler.handleHtmlAjax(pRequest, pResponse);
	   } else if (pathInfo.equals("/login")) {
		   HTTPListenerHelper helper = new HTTPListenerHelper();
		   helper.doLogin(pRequest, pResponse);
	   } else if (pathInfo.equals("/checkclass")) {
		   HTTPListenerHelper helper = new HTTPListenerHelper();
		   helper.checkClass(pRequest, pResponse, this.getClass());
	   } else {
		   throw new ServletException("Unknown get request " + pRequest.getRequestURI());
	   }
   }

    /**
     * Method that gets invoked when the servlet is destroyed
     */
    public void destroy(){
        super.destroy();
    }

}