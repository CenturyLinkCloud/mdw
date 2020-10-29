package com.centurylink.mdw.hub;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.WebSocketMessenger;
import com.centurylink.mdw.services.util.InitialRequest;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.websocket.server.ServerContainer;

@WebListener
public class StartupListener implements ServletContextListener {

    private static MdwMain mdwMain;

    @Override
    public void contextInitialized(ServletContextEvent contextEvent) {
        ServletContext servletContext = contextEvent.getServletContext();
        mdwMain = new MdwMain();
        String container = "Tomcat"; // TODO
        if (ApplicationContext.isSpringBoot()) {
            ServerContainer serverContainer = (ServerContainer)servletContext.getAttribute("javax.websocket.server.ServerContainer");
            try {
                serverContainer.addEndpoint(WebSocketMessenger.class);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            mdwMain.startup(container, SpringBootApplication.getBootDir().toString(), servletContext.getContextPath());
        }
        else {
            mdwMain.startup(container, servletContext.getRealPath("/"), servletContext.getContextPath());
            new InitialRequest().submit();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent contextEvent) {
        if (mdwMain != null)
            mdwMain.shutdown();
    }

}
