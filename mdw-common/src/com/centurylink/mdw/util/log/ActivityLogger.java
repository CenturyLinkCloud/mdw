package com.centurylink.mdw.util.log;

import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;

public class ActivityLogger extends AbstractStandardLoggerBase {

    private ActivityRuntimeContext runtimeContext;

    public ActivityLogger(ActivityRuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }

    @Override
    public void info(String msg) {
        runtimeContext.logInfo(msg);
    }

    @Override
    public void warn(String msg) {
        runtimeContext.logWarn(msg);
    }

    @Override
    public void error(String msg) {
        runtimeContext.logError(msg);
    }

    @Override
    public void severe(String msg) {
        runtimeContext.logSevere(msg);
    }

    @Override
    public void debug(String msg) {
        runtimeContext.logDebug(msg);
    }

    @Override
    public void trace(String msg) {
        runtimeContext.logTrace(msg);
    }

    @Override
    public void mdwDebug(String msg) {
        trace(msg);
    }

    @Override
    public void info(String msg, Throwable t) {
        if (isInfoEnabled())
            error(msg, t);
    }

    @Override
    public void infoException(String msg, Throwable t) {
        info(msg, t);
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (isEnabledFor(LogLevel.WARN))
            error(msg, t);
    }

    @Override
    public void warnException(String msg, Throwable t) {
        warn(msg, t);
    }

    @Override
    public void error(String msg, Throwable t) {
        runtimeContext.logError(msg, t);
    }

    @Override
    public void severeException(String msg, Throwable t) {
        runtimeContext.logException(msg, t);

    }

    @Override
    public void debug(String msg, Throwable t) {
        if (isDebugEnabled())
            error(msg, t);
    }

    @Override
    public void debugException(String msg, Throwable t) {
        debug(msg, t);
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (isTraceEnabled())
            error(msg, t);
    }

    @Override
    public void traceException(String msg, Throwable t) {
        trace(msg, t);
    }

    @Override
    public boolean isEnabledFor(LogLevel level) {
        if (level == LogLevel.INFO)
            return runtimeContext.isLogInfoEnabled();
        else if (level == LogLevel.DEBUG)
            return runtimeContext.isLogDebugEnabled();
        else if (level == LogLevel.TRACE)
            return runtimeContext.isLogTraceEnabled();
        return true;
    }

    @Override
    public void log(LogLevel level, String message) {
        if (level == LogLevel.INFO)
            info(message);
        else if (level == LogLevel.DEBUG)
            debug(message);
        else if (level == LogLevel.TRACE)
            trace(message);
        else if (level == LogLevel.WARN)
            warn(message);
        else
            error(message);
    }
}
