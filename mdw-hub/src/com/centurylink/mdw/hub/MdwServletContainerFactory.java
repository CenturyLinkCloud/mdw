package com.centurylink.mdw.hub;

import org.apache.catalina.Context;
import org.apache.tomcat.util.descriptor.web.ErrorPage;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;

import java.io.File;

public class MdwServletContainerFactory extends TomcatServletWebServerFactory {

    public MdwServletContainerFactory() {
    }

    public MdwServletContainerFactory(String contextRoot, int port, File bootDir) {
        super(contextRoot, port);
        addContextCustomizers(new ContextCustomizer());
        setDocumentRoot(new File(bootDir + "/web"));
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
}
