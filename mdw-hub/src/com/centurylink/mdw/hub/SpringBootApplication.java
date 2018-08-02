/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.hub;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.catalina.Context;
import org.apache.tomcat.util.descriptor.web.ErrorPage;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.util.ClasspathUtil;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.file.ZipHelper;

@Configuration
@ComponentScan
@ServletComponentScan
@ManagedResource(objectName="com.centurylink.mdw.springboot:name=application")
public class SpringBootApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(SpringBootApplication.class, args);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * TODO: support Jetty as well
     */
    @Bean
    public EmbeddedServletContainerFactory embeddedServletContainerFactory(ApplicationContext ctx) {
        String portProp = System.getProperty("mdw.server.port");
        if (portProp == null)
            portProp = System.getProperty("server.port");
        if (portProp == null)
            portProp = "8080";
        String contextProp = System.getProperty("mdw.server.contextPath");
        if (contextProp == null)
            contextProp = System.getProperty("server.contextPath");
        if (contextProp == null)
            contextProp = "/mdw";
        TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory(
                contextProp, Integer.parseInt(portProp));
        factory.addContextCustomizers(tomcatContextCustomizer());
        factory.setDocumentRoot(new File(getBootDir() + "/web"));
        return factory;
    }

    @Bean
    public TomcatContextCustomizer tomcatContextCustomizer() {
        return new TomcatContextCustomizer() {
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
        };
    }

    private static File bootDir;
    protected static synchronized File getBootDir() throws StartupException {
        if (bootDir == null) {
            String bootLoc = System.getProperty("mdw.boot.dir");
            if (bootLoc == null) {
                String tempLoc = PropertyManager.getProperty(PropertyNames.MDW_TEMP_DIR);
                if (tempLoc == null)
                    tempLoc = "mdw/temp";
                bootLoc = tempLoc + "/boot";
            }
            String mainLoc = ClasspathUtil.locate(MdwMain.class.getName());
            try {
                bootDir = new File(bootLoc);
                if (bootDir.isDirectory())
                    FileHelper.deleteRecursive(bootDir);
                if (!bootDir.mkdirs())
                    throw new StartupException("Cannot create boot dir: " + bootDir.getAbsolutePath());
                File bootJar = null;
                if (mainLoc.indexOf('!') > 0) {
                    bootJar = new File(new URI(mainLoc.substring(0, mainLoc.indexOf('!'))));
                    if (!bootJar.exists())
                        throw new StartupException("No Spring Boot jar: " + mainLoc);
                    System.out.println("Spring Boot Jar => " + bootJar.getAbsolutePath());
                    ZipHelper.unzip(bootJar, bootDir);
                }
                else  // PCF deployment explodes the JAR already
                    bootDir = new File(mainLoc.substring(0, mainLoc.indexOf("BOOT-INF")));
            }
            catch (IOException ex) {
                throw new StartupException(ex.getMessage(), ex);
            }
            catch (URISyntaxException ex) {
                throw new StartupException("Cannot interpret main location: " + mainLoc);
            }
        }
        return bootDir;
    }
}
