/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities.logger;

import java.io.Serializable;

import com.centurylink.mdw.common.utilities.logger.log4j.Log4JStandardLoggerImpl;

public class LoggerUtil implements Serializable {

    public static final String MDW_LOGGER_IMPL = "mdw.logger.impl";


    private static LoggerUtil util = new LoggerUtil();

    /**
     * Private method to restrict the instance to be a single one only.
     */
    private LoggerUtil() {
    }

    /**
     * Static method to get instance
     */
    public static LoggerUtil getInstance() {
        return util;
    }

    private static boolean accessed;

    /**
     * Get a standard logger.
     */
    public static StandardLogger getStandardLogger() {
        String loggerImplClass = System.getProperty(MDW_LOGGER_IMPL);
        if (!accessed && loggerImplClass != null) {
            System.out.println("\nUsing Logger Impl: " + loggerImplClass);
            accessed = true;
        }
        // avoid reflection for known impls
        if (loggerImplClass == null || SimpleLogger.class.getName().equals(loggerImplClass)) {
            return SimpleLogger.getSingleton();
        }
        else if (Log4JStandardLoggerImpl.class.getName().equals(loggerImplClass) || org.apache.log4j.Logger.class.getName().equals(loggerImplClass)) {
            return new Log4JStandardLoggerImpl();
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
}
