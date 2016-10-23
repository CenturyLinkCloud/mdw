/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util.log;

import java.io.PrintStream;
import java.io.Serializable;

public interface StandardLogger extends Serializable
{
    public enum LogLevel { INFO, WARN, DEBUG, ERROR, TRACE };

    /**
     * Logs messages which are of level INFO.
     * INFO is a message level for informational messages.
     * Typically INFO messages will be written to the console
     * or its equivalent.  So the INFO level should only be
     * used for reasonably significant messages that will
     * make sense to end users and system admins.
     *
     * @param message the message to be logged
     */
    void info(java.lang.String logtodisplay);

    /**
      * Logs messages which are of level WARN.
     * WARN is a message level indicating a potential problem.
     * In general WARNING messages should describe events that will
     * be of interest to end users or system managers, or which
     * indicate potential problems.
     *
     * @param message the message to be logged
     */
    void warn(java.lang.String logtodisplay);

    /**
      * Logs messages which are of level SEVERE.
      * SEVERE is a message level indicating a serious failure.
      * In general SEVERE messages should describe events that are
      * of considerable importance and which will prevent normal
      * program execution.   They should be reasonably intelligible
      * to end users and to system administrators.
      *
      * @param message the message to be logged
      */
    void severe(java.lang.String logtodisplay);

    void trace(String logtoDisplay);

    void traceException(String msg, Throwable t);

    /**
     * INFO is a message level for informational messages.
     * Typically INFO messages will be written to the console
     * or its equivalent.  So the INFO level should only be
     * used for reasonably significant messages that will
     * make sense to end users and system admins.
     * The message of the Throwable [getMessage()] is attached to the log message.
     *
     * @param message   the message to be logged
     * @param throwable the throwable to be looged
     */
    void infoException(java.lang.String logtodisplay,
        java.lang.Throwable throwable);

    /**
      * Logs messages which are of level SEVERE.
      * SEVERE is a message level indicating a serious failure.
      * In general SEVERE messages should describe events that are
      * of considerable importance and which will prevent normal
      * program execution.   They should be reasonably intelligible
      * to end users and to system administrators.
      *
      * @param message    the message to be logged.
      * @param throwable  the throwable to be logged.
      */
    void severeException(java.lang.String message,
        java.lang.Throwable throwable);

    /**
      * Logs messages which are of level WARN.
      * WARN is a message level indicating a potential problem.
      * In general WARNING messages should describe events that will
      * be of interest to end users or system managers, or which
      * indicate potential problems.
      * The message of the Throwable [getMessage()] is attached to the log message.
      *
      * @param message   the message to be logged
      * @param throwable the throwable to be logged
      */
    void warnException(java.lang.String logtodisplay,
        java.lang.Throwable throwable);



    /**
      * Logs messages which are of level DEBUG.
      * DEBUG is a message level providing tracing information..
      * Typically used for debugging purposes.
      *
      * @param message the message to be logged
      */
    void debug(java.lang.String logtodisplay);

    /**
      * Logs messages which are of level Debug.
      * DEBUG is a message level providing tracing information..
      * Typically used for debugging purposes.
      * The message of the Throwable [getMessage()] is attached to the log message.
      *
      * @param message    the message to be logged
      * @param throwable  the throwable to be logged
    */
    void debugException(java.lang.String msg, java.lang.Throwable throwable);

    /**
     * Checks if the debug log vele is set or not
     * @return boolean status
     */
    boolean isDebugEnabled();

     /**
     * Checks if the info log vele is set or not
     * @return boolean status
     */
    boolean isInfoEnabled();

    boolean isMdwDebugEnabled();

    boolean isTraceEnabled();

    void mdwDebug(String message);

    public boolean isEnabledFor(LogLevel level);

    public void log(LogLevel level, String message);

    public void refreshCache();

    public void exception(String tag, String message, Throwable e);

    public void info(String tag, String message);

    public void debug(String tag, String message);

    public void warn(String tag, String message);

    public void severe(String tag, String message);

    public void trace(String tag, String message);

    public String getDefaultHost();

    public String getDefaultPort();

    public String getSentryMark();

    public PrintStream getPrintStream();
}
