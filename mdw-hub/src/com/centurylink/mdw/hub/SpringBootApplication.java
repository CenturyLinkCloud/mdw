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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.util.ClasspathUtil;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.file.ZipHelper;

@Configuration
@ComponentScan
@ServletComponentScan
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
        factory.setDocumentRoot(new File(getBootDir() + "/web"));
        return factory;
    }

    private static File bootDir;
    protected static synchronized File getBootDir() throws StartupException {
        if (bootDir == null) {
            String bootLoc = System.getProperty("mdw.boot.dir");
            if (bootLoc == null) {
                String tempLoc = PropertyManager.getProperty(PropertyNames.MDW_TEMP_DIR);
                if (tempLoc == null)
                    tempLoc = "mdw/.temp";
                bootLoc = tempLoc + "/boot";
            }
            String mainLoc = ClasspathUtil.locate(MdwMain.class.getName());
            try {
                bootDir = new File(bootLoc);
                if (bootDir.isDirectory())
                    FileHelper.deleteRecursive(bootDir);
                if (!bootDir.mkdirs())
                    throw new StartupException("Cannot create boot dir: " + bootDir.getAbsolutePath());

                File bootJar = new File(new URI(mainLoc.substring(0, mainLoc.indexOf('!'))));
                if (!bootJar.exists())
                    throw new StartupException("No Spring Boot jar: " + mainLoc);
                System.out.println("Spring Boot Jar: " + bootJar.getAbsolutePath());
                ZipHelper.unzip(bootJar, bootDir);
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
