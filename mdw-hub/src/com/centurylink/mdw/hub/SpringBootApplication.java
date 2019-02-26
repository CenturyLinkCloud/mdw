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

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.services.util.InitialRequest;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.util.ClasspathUtil;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.file.ZipHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@ComponentScan
@ServletComponentScan
@ManagedResource(objectName="com.centurylink.mdw.springboot:name=application")
public class SpringBootApplication {

    public static void main(String[] args) {

        try {
            // must be called before Spring Boot starts logging
            LoggerUtil.initializeLogging();
            SpringApplication.run(SpringBootApplication.class, args);
            new InitialRequest().submit();
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Bean
    public TomcatServletWebServerFactory containerFactory() {
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
        return new MdwServletContainerFactory(contextProp, Integer.parseInt(portProp), getBootDir());
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
                File bootJar;
                if (mainLoc.indexOf('!') > 0) {
                    bootJar = new File(new URI(mainLoc.substring(0, mainLoc.indexOf('!'))));
                    if (!bootJar.exists())
                        throw new StartupException("No Spring Boot jar: " + mainLoc);
                    System.out.println("Spring Boot Jar => " + bootJar.getAbsolutePath());
                    ApplicationContext.setBootJar(bootJar);
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
