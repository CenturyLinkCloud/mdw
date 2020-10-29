package com.centurylink.mdw.util.log;

public interface StandardLogger
{
    enum LogLevel { INFO, WARN, DEBUG, ERROR, TRACE }

    void info(String msg);

    void warn(String msg);

    default void error(String msg) {
        severe(msg);
    }
    @Deprecated
    void severe(String msg);

    void debug(String msg);

    void trace(String msg);

    void mdwDebug(String msg);

    default void info(String msg, Throwable t) {
        infoException(msg, t);
    }
    @Deprecated
    void infoException(String msg, Throwable t);

    default void warn(String msg, Throwable t) {
        warnException(msg, t);
    }
    @Deprecated
    void warnException(String msg, Throwable t);

    default void error(String msg, Throwable t) {
        severeException(msg, t);
    }
    @Deprecated
    void severeException(String msg, Throwable t);

    default void debug(String msg, Throwable t) {
        debugException(msg, t);
    }
    @Deprecated
    void debugException(String msg, Throwable t);

    default void trace(String msg, Throwable t) {
        traceException(msg, t);
    }
    @Deprecated
    void traceException(String msg, Throwable t);

    boolean isInfoEnabled();
    boolean isDebugEnabled();
    boolean isTraceEnabled();
    boolean isMdwDebugEnabled();

    boolean isEnabledFor(LogLevel level);

    void log(LogLevel level, String message);

    void info(String tag, String message);
    void warn(String tag, String message);
    @Deprecated
    void exception(String tag, String message, Throwable e);
    default void error(String tag, String message, Throwable e) {
        exception(tag, message, e);
    }
    @Deprecated
    void severe(String tag, String message);
    default void error(String tag, String message) {
        severe(tag, message);
    }
    void debug(String tag, String message);
    void trace(String tag, String message);

    String getDefaultHost();
    int getDefaultPort();

    void refreshWatcher();
}
