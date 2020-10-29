package com.centurylink.mdw.boot.servlet;

import com.centurylink.mdw.boot.MdwStarter;
import com.centurylink.mdw.common.service.WebSocketMessenger;
import com.centurylink.mdw.container.ContextProvider;
import com.centurylink.mdw.hub.MdwMain;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.websocket.server.ServerContainer;

@WebListener
public class StartupListener implements ServletContextListener {

    private static MdwMain mdwMain;

    @Autowired
    private MdwStarter mdwStarter;

    @Override
    public void contextInitialized(ServletContextEvent contextEvent) {
        ServletContext servletContext = contextEvent.getServletContext();
        mdwMain = new MdwMain();
        String container = "Tomcat"; // TODO
        ServerContainer serverContainer = (ServerContainer)servletContext.getAttribute("javax.websocket.server.ServerContainer");
        try {
            serverContainer.addEndpoint(WebSocketMessenger.class);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        // mdw-spring-boot
        mdwMain.startup(container, mdwStarter.getBootDir().toString(), servletContext.getContextPath());
    }

    @Override
    public void contextDestroyed(ServletContextEvent contextEvent) {
        if (mdwMain != null)
            mdwMain.shutdown();
    }

}
