package com.centurylink.mdw.boot;

import com.centurylink.mdw.hub.MdwServletContainerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
public class CustomTomcat implements WebServerFactoryCustomizer<TomcatServletWebServerFactory>,
        ApplicationListener<ContextClosedEvent> {

    private MdwServletContainerFactory.ConnectorCustomizer connectorCustomizer;

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        connectorCustomizer = new MdwServletContainerFactory.ConnectorCustomizer();
        factory.addConnectorCustomizers(connectorCustomizer);
    }

    public void onApplicationEvent(ContextClosedEvent event) {
        connectorCustomizer.shutdownTomcatThreadpool();
    }
}