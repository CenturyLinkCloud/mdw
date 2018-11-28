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

public interface StandardLogger
{
    public enum LogLevel { INFO, WARN, DEBUG, ERROR, TRACE };

    void info(String msg);

    void warn(String msg);

    default void error(String msg) {
        severe(msg);
    }
    void severe(String msg);

    void debug(String msg);

    void trace(String msg);

    void mdwDebug(String message);

    default void info(String msg, Throwable t) {
        infoException(msg, t);
    }
    void infoException(String msg, Throwable t);

    default void warn(String msg, Throwable t) {
        warnException(msg, t);
    }
    void warnException(String msg, Throwable t);

    default void error(String msg, Throwable t) {
        severeException(msg, t);
    }
    void severeException(String msg, Throwable t);

    default void debug(String msg, Throwable t) {
        debugException(msg, t);
    }
    void debugException(String msg, Throwable t);

    default void trace(String msg, Throwable t) {
        traceException(msg, t);
    }
    void traceException(String msg, Throwable t);

    boolean isInfoEnabled();
    boolean isDebugEnabled();
    boolean isTraceEnabled();
    boolean isMdwDebugEnabled();

    public boolean isEnabledFor(LogLevel level);

    public void log(LogLevel level, String message);

    public void info(String tag, String message);
    public void warn(String tag, String message);
    public void exception(String tag, String message, Throwable e);
    public void severe(String tag, String message);
    public void debug(String tag, String message);
    public void trace(String tag, String message);

    public String getDefaultHost();
    public String getDefaultPort();
}
