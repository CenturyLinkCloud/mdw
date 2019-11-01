package com.centurylink.mdw.services.process;

import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.service.data.WorkflowDataAccess;
import com.centurylink.mdw.util.log.AbstractStandardLoggerBase;
import com.centurylink.mdw.util.log.LoggerUtil;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ActivityLogger extends AbstractStandardLoggerBase {

    private ActivityRuntimeContext runtimeContext;

    public ActivityLogger(ActivityRuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }

    @Override
    public void info(String msg) {
        runtimeContext.logInfo(msg);
        persist(LogLevel.INFO, msg);
    }

    @Override
    public void warn(String msg) {
        runtimeContext.logWarn(msg);
        persist(LogLevel.WARN, msg);
    }

    @Override
    public void error(String msg) {
        runtimeContext.logError(msg);
        persist(LogLevel.ERROR, msg);
    }

    @Override
    public void severe(String msg) {
        runtimeContext.logSevere(msg);
        persist(LogLevel.ERROR, msg);
    }

    @Override
    public void debug(String msg) {
        runtimeContext.logDebug(msg);
        persist(LogLevel.DEBUG, msg);
    }

    /**
     * does not persist
     */
    @Override
    public void trace(String msg) {
        runtimeContext.logTrace(msg);
    }

    /**
     * does not persist
     */
    @Override
    public void mdwDebug(String msg) {
        trace(msg);
    }

    @Override
    public void info(String msg, Throwable t) {
        if (isInfoEnabled()) {
            error(msg, t);
        }
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
        persist(LogLevel.ERROR, msg, t);
    }

    @Override
    public void severeException(String msg, Throwable t) {
        runtimeContext.logException(msg, t);
        persist(LogLevel.ERROR, msg, t);
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

        persist(level, message);
    }

    private void persist(LogLevel level, String message) {
        persist(level, message, null);
    }

    private void persist(LogLevel level, String message, Throwable t) {
        persist(runtimeContext.getActivityInstanceId(), level, message, t);
    }

    static void persist(Long activityInstanceId, LogLevel level, String message) {
        persist(activityInstanceId, level, message, null);
    }

    static void persist(Long activityInstanceId, LogLevel level, String message, Throwable t) {
        boolean isLogging = false;  // TODO
        if (isLogging) {
            try {
                WorkflowDataAccess dataAccess = new WorkflowDataAccess();
                dataAccess.addActivityLog(activityInstanceId, level.toString(), message);
                if (t != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    t.printStackTrace(new PrintStream(baos));
                    String stackTrace = new String(baos.toByteArray());
                    if (stackTrace.length() > 3997) {
                        stackTrace = stackTrace.substring(0, 3997) + "...";
                    }
                    dataAccess.addActivityLog(activityInstanceId, level.toString(), stackTrace);
                }
            }
            catch (DataAccessException ex) {
                // don't try and use this logger
                LoggerUtil.getStandardLogger().error(ex.getMessage(), ex);
            }
        }
    }
}
