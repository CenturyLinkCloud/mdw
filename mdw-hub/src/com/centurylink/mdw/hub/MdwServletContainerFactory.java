package com.centurylink.mdw.hub;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.util.log.LoggerUtil;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.ErrorPage;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MdwServletContainerFactory extends TomcatServletWebServerFactory {

    public MdwServletContainerFactory() {
    }

    public MdwServletContainerFactory(String contextRoot, int port, File bootDir) {
        super(contextRoot, port);
        addContextCustomizers(new ContextCustomizer());
        connectorCustomizer = new ConnectorCustomizer();
        addConnectorCustomizers(connectorCustomizer);
        setDocumentRoot(new File(bootDir + "/web"));
    }

    private ConnectorCustomizer connectorCustomizer;
    public ConnectorCustomizer getConnectorCustomizer() {
        return connectorCustomizer;
    }

    class ContextCustomizer implements TomcatContextCustomizer {
        @Override
        public void customize(Context context) {
            context.addApplicationListener("org.apache.tomcat.websocket.server.WsContextListener");
            context.addErrorPage(new ErrorPage() {
                @Override
                public int getErrorCode() {
                    return 404;
                }

                @Override
                public String getLocation() {
                    return "/404";
                }
            });
            context.addErrorPage(new ErrorPage() {
                @Override
                public int getErrorCode() {
                    return 500;
                }

                @Override
                public String getLocation() {
                    return "/error";
                }
            });

            // CORS access is wide open
            FilterDef corsFilter = new FilterDef();
            corsFilter.setFilterName("CorsFilter");
            corsFilter.setFilterClass("org.apache.catalina.filters.CorsFilter");
            corsFilter.addInitParameter("cors.allowed.methods", "GET,POST,PUT,DELETE,HEAD,OPTIONS");
            corsFilter.addInitParameter("cors.allowed.headers", "Authorization,Content-Type,X-Requested-With,Accept,Origin,Access-Control-Request-Method,Access-Control-Request-Headers,Accept-Encoding,Accept-Language,Cache-Control,Connection,Host,Pragma,Referer,User-Agent");
            corsFilter.addInitParameter("cors.allowed.origins", "*");
            context.addFilterDef(corsFilter);
            FilterMap filterMap = new FilterMap();
            filterMap.setFilterName(corsFilter.getFilterName());
            filterMap.addURLPattern("/api/*");
            filterMap.addURLPattern("/services/AppSummary");
            context.addFilterMap(filterMap);
        }
    }

    class ConnectorCustomizer implements TomcatConnectorCustomizer {
        private volatile Connector connector;

        @Override
        public void customize(Connector connector) {
            System.out.println("CUSTOMIZE CUSTOMIZE");
            this.connector = connector;
        }

        public void shutdownTomcatThreadpool() {
            this.connector.pause();
            int timeout = PropertyManager.getIntegerProperty(PropertyNames.MDW_THREADPOOL_TERMINATION_TIMEOUT, 60);
            Executor executor = this.connector.getProtocolHandler().getExecutor();
            if (executor instanceof ThreadPoolExecutor) {
                try {
                    ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
                    threadPoolExecutor.shutdown();
                    if (!threadPoolExecutor.awaitTermination(timeout, TimeUnit.SECONDS)) {
                        LoggerUtil.getStandardLogger().error("Thread pool fails to terminate after " + timeout + " seconds");
                    }
                }
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
