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
package com.centurylink.mdw.util.log.log4j;

import com.centurylink.mdw.util.log.AbstractStandardLoggerBase;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Log4JStandardLoggerImpl extends AbstractStandardLoggerBase {

    private Logger selfLogger = Logger.getLogger(Log4JStandardLoggerImpl.class);
    private Logger logger;

    // avoid missed logging messages due to missing appender registration
    public static final String DEFAULT_LOGGER_NAME = "com.centurylink.mdw.logging";

    public Log4JStandardLoggerImpl() {
        try {
            String className = getCallingClassName();
            logger = Logger.getLogger(className);
        } catch (Throwable th) {
            logger = Logger.getLogger(DEFAULT_LOGGER_NAME);
            selfLogger.debug("No logger defined", th);
        }
    }

    public Log4JStandardLoggerImpl(String className) {
        logger = Logger.getLogger(className);
    }

    public void debug(String logtodisplay) {
        logIt(LogLevel.DEBUG, logtodisplay, null);
    }

    public void debugException(String msg, Throwable t) {
        logIt(LogLevel.DEBUG, msg, t);
    }

    public void info(String msg) {
        logIt(LogLevel.INFO, msg, null);
    }

    public void trace(String msg) {
        logIt(LogLevel.TRACE, msg, null);
    }

    public void traceException(String msg, Throwable t) {
        logIt(LogLevel.TRACE, msg, t);
     }

    public void infoException(String msg, Throwable t) {
        logIt(LogLevel.INFO, msg, t);
    }

    public boolean isDebugEnabled() {
        return (logger.isDebugEnabled());
    }

    public boolean isInfoEnabled() {
        return (logger.isInfoEnabled());
    }

    public boolean isTraceEnabled() {
        return logger.isEnabledFor(Level.toLevel(Level.TRACE_INT));
    }

    public void severe(String msg) {
        logIt(LogLevel.ERROR, msg, null);
    }

    public void severeException(String msg, Throwable t) {
        logIt(LogLevel.ERROR, msg, t);
    }

    public void warn(String msg) {
        logIt(LogLevel.WARN, msg, null);
    }

    public void warnException(String msg, Throwable t) {
        logIt(LogLevel.WARN, msg, t);
    }

   /**
    *  Get the name of the class to use as the Logger name
    */
    private String getCallingClassName() {
        StackTraceElement[] stack = (new Throwable()).getStackTrace();
        String className = stack[4].getClassName();

        if (className == null || !(className.startsWith("com.centurylink") || className.startsWith("com.qwest"))) {
            className = stack[3].getClassName();  // try next level up
            if (className == null || !(className.startsWith("com.centurylink") || className.startsWith("com.qwest"))) {
                selfLogger.debug("Unfamiliar Log4J Logger: '" + className + "'; using Default '" + DEFAULT_LOGGER_NAME + "'");
                className = DEFAULT_LOGGER_NAME;
            }
        }

        return className;
    }

    public boolean isMdwDebugEnabled() {
        return (logger.isTraceEnabled());
    }

    public void mdwDebug(String message) {
        if (isTraceEnabled()) {
             String line = generate_log_line('d', null, message);
             logIt(LogLevel.TRACE, line, null);
        }
    }

    public boolean isEnabledFor(LogLevel level) {
        return logger.isEnabledFor(Level.toLevel(level.toString()));
    }

    public void log(LogLevel level, String message) {
        if (isEnabledFor(level)) {
            logger.log(Level.toLevel(level.toString()), message);
        }
    }
}