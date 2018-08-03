/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.boot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.hub.context.ContextPaths;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.util.ClasspathUtil;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.file.ZipHelper;
import com.centurylink.mdw.util.file.ZipHelper.Exist;

@Configuration
@ComponentScan
public class AutoConfig {

    @Bean
    public MdwStarter getMdwStarter() {
        return new MdwStarter(getBootDir());
    }

    @Bean
    public ContextPaths getContextPaths() {
        return new ContextPaths();
    }

    private static File bootDir;
    private static synchronized File getBootDir() throws StartupException {
        if (bootDir == null) {
            String bootLoc = System.getProperty("mdw.boot.dir");
            if (bootLoc == null) {
                String tempLoc = PropertyManager.getProperty(PropertyNames.MDW_TEMP_DIR);
                if (tempLoc == null)
                    tempLoc = "mdw/temp";
                bootLoc = tempLoc + "/boot";
            }
            String classLoc = ClasspathUtil.locate(AutoConfig.class.getName());
            try {
                bootDir = new File(bootLoc);
                if (bootDir.isDirectory())
                    FileHelper.deleteRecursive(bootDir);
                if (!bootDir.mkdirs())
                    throw new StartupException("Cannot create boot dir: " + bootDir.getAbsolutePath());
                ClassLoader classLoader = AutoConfig.class.getClassLoader();
                if (classLoc.indexOf('!') > 0) {
                    File bootJar = new File(new URI(classLoc.substring(0, classLoc.indexOf('!'))));
                    if (!bootJar.exists())
                        throw new StartupException("No Spring Boot jar: " + classLoc);
                    System.out.println("Spring Boot Jar => " + bootJar.getAbsolutePath());
                    ZipHelper.unzip(bootJar, bootDir);
                }
                else  {
                    // deployment explodes the JAR already
                    bootDir = new File(classLoc.substring(0, classLoc.indexOf("BOOT-INF")));
                }

                // explode mdw-spring-boot on top
                String filename = "mdw-spring-boot-" + ApplicationContext.getMdwVersion() + ".jar";
                try (InputStream is = classLoader.getResourceAsStream("BOOT-INF/lib/" + filename)) {
                    File bootJar = new File(bootDir + "/" + filename);
                    Files.copy(is, Paths.get(bootJar.getPath()), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("MDW Spring Boot Jar => " + bootJar.getAbsolutePath());
                    ZipHelper.unzip(bootJar, bootDir, null, null, Exist.Overwrite);
                }
            }
            catch (IOException ex) {
                throw new StartupException(ex.getMessage(), ex);
            }
            catch (URISyntaxException ex) {
                throw new StartupException("Cannot interpret main location: " + classLoc);
            }
        }
        return bootDir;
    }
}
