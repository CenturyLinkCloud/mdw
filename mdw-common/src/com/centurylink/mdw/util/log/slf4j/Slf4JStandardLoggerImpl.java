package com.centurylink.mdw.util.log.slf4j;

import com.centurylink.mdw.util.log.AbstractStandardLoggerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.Util;

public class Slf4JStandardLoggerImpl extends AbstractStandardLoggerBase {

    private Logger logger;

    public Slf4JStandardLoggerImpl() {
        logger = LoggerFactory.getLogger(Util.getCallingClass());
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void severe(String msg) {
        logger.error(msg);
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }

    @Override
    public void trace(String msg) {
        logger.trace(msg);
    }

    @Override
    public void mdwDebug(String msg) {
        logger.trace(msg);
    }

    @Override
    public void infoException(String msg, Throwable t) {
        logger.info(msg, t);
    }

    @Override
    public void warnException(String msg, Throwable t) {
        logger.warn(msg, t);
    }

    @Override
    public void severeException(String msg, Throwable t) {
        logger.error(msg, t);
    }

    @Override
    public void debugException(String msg, Throwable t) {
        logger.debug(msg);
    }

    @Override
    public void traceException(String msg, Throwable t) {
        logger.trace(msg, t);
    }

    @Override
    public boolean isEnabledFor(LogLevel level) {
        if (level == LogLevel.INFO)
            return logger.isInfoEnabled();
        else if (level == LogLevel.DEBUG)
            return logger.isDebugEnabled();
        else if (level == LogLevel.TRACE)
            return logger.isTraceEnabled();
        return true;
    }

    @Override
    public void log(LogLevel level, String msg) {
        if (isEnabledFor(level)) {
            if (level == LogLevel.INFO)
                logger.info(msg);
            else if (level == LogLevel.WARN)
                logger.warn(msg);
            else if (level == LogLevel.ERROR)
                logger.error(msg);
            else if (level == LogLevel.DEBUG)
                logger.debug(msg);
            else if (level == LogLevel.TRACE)
                logger.trace(msg);
        }
    }
}
