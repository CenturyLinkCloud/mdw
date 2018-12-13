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
package com.centurylink.mdw.util.log;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.config.YamlProperties;
import com.centurylink.mdw.util.log.log4j.Log4JStandardLoggerImpl;
import com.centurylink.mdw.util.log.slf4j.Slf4JStandardLoggerImpl;
import org.slf4j.helpers.Util;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class LoggerUtil implements Serializable {

    public static final String MDW_LOGGER_IMPL = "mdw.logger.impl";

    private static LoggerUtil util = new LoggerUtil();

    private LoggerUtil() {
    }

    public static LoggerUtil getInstance() {
        return util;
    }

    private static boolean accessed;


    /**
     * For Spring Boot with slf4j SimpleLogger, an opportunity to initialize logging before Spring starts logging.
     * Otherwise for slf4j, properties have already been defaulted before we have a chance to set them.
     *
     * See mdw-spring-boot AutoConfig for initialization of logback for pure Spring Boot apps.
     */
    public static String initializeLogging() throws IOException {
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyyMMdd.HH:mm:ss.SSS");
        // Peek at mdw.yaml to find default logging level, but do not initialize PropertyManager
        // which could initialize slf4j prematurely.  (Works only with yaml config file).
        String mdwLogLevel = null;
        File mdwYaml = getConfigurationFile("mdw.yaml");
        if (mdwYaml.exists()) {
            YamlProperties yamlProps = new YamlProperties("mdw", mdwYaml);
            mdwLogLevel = yamlProps.getString("mdw.logging.level");
            if (mdwLogLevel != null) {
                if (mdwLogLevel.equals("MDW_DEBUG"))
                    mdwLogLevel = "TRACE";
                System.setProperty("org.slf4j.simpleLogger.log.com.centurylink.mdw", mdwLogLevel.toLowerCase());
            }
        }
        return mdwLogLevel;
    }

    public static StandardLogger getStandardLogger() {
        String loggerImplClass = System.getProperty(MDW_LOGGER_IMPL);
        if (!accessed && loggerImplClass != null) {
            System.out.println("\nUsing Logger Impl: " + loggerImplClass);
            accessed = true;
        }
        // avoid reflection for known impls
        if (loggerImplClass == null) {
            return new Slf4JStandardLoggerImpl(Util.getCallingClass().getName());
        }
        else if (SimpleLogger.class.getName().equals(loggerImplClass)) {
            return SimpleLogger.getSingleton();
        }
        else if (Log4JStandardLoggerImpl.class.getName().equals(loggerImplClass) || org.apache.log4j.Logger.class.getName().equals(loggerImplClass)) {
            return new Log4JStandardLoggerImpl();
        }
        else if (Slf4JStandardLoggerImpl.class.getName().equals(loggerImplClass) || org.slf4j.Logger.class.getName().equals(loggerImplClass)) {
            return new Slf4JStandardLoggerImpl(Util.getCallingClass().getName());
        }
        else {
            try {
                return Class.forName(loggerImplClass).asSubclass(StandardLogger.class).newInstance();
            }
            catch (Exception ex) {
                ex.printStackTrace();  // logging isn't working
                return null;
            }
        }
    }

    /**
     * Get a standard logger based on a class name.
     */
    public static StandardLogger getStandardLogger(String className) {
        String loggerImplClass = System.getProperty(MDW_LOGGER_IMPL);
        if (!accessed && loggerImplClass != null) {
            System.out.println("\nUsing Logger Impl: " + loggerImplClass);
            accessed = true;
        }
        // avoid reflection for known impls
        if (loggerImplClass == null || className == null || SimpleLogger.class.getName().equals(loggerImplClass)) {
            return getStandardLogger();
        }
        else if (Log4JStandardLoggerImpl.class.getName().equals(loggerImplClass) || org.apache.log4j.Logger.class.getName().equals(loggerImplClass)) {
            return new Log4JStandardLoggerImpl(className);
        }
        else if (Slf4JStandardLoggerImpl.class.getName().equals(loggerImplClass) || org.slf4j.Logger.class.getName().equals(loggerImplClass)) {
            return new Slf4JStandardLoggerImpl(className);
        }
        else {
            try {
                Class<? extends StandardLogger> loggerClass = Class.forName(loggerImplClass).asSubclass(StandardLogger.class);
                return loggerClass.getConstructor(String.class).newInstance(className);
            }
            catch (Exception ex) {
                ex.printStackTrace();  // logging isn't working
                return null;
            }
        }
    }

    public static File getConfigurationFile(String filepath) {
        String configDir = System.getProperty(PropertyManager.MDW_CONFIG_LOCATION);
        if (configDir == null)
            return new File(filepath);
        else if (configDir.endsWith("/"))
            return new File(configDir + filepath);
        else
            return new File(configDir + "/" + filepath);
    }

}
