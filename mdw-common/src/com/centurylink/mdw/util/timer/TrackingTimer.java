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
package com.centurylink.mdw.util.timer;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;

public class TrackingTimer extends CodeTimer {

    private String logTag;
    private StandardLogger logger;
    private LogLevel logLevel;
    private String unqualifiedClassName;

    public TrackingTimer(String logTag, String className, LogLevel logLevel) {
        super("", false);
        this.logTag = logTag;
        this.logger = LoggerUtil.getStandardLogger(className);
        this.unqualifiedClassName = className.substring(className.lastIndexOf('.') + 1);
        this.logLevel = logLevel;
    }

    public void start(String message) {
        super.setLabel(message);
        super.start();
    }

    public void stopAndLogTiming(String note) {
        stop(note);
        if (logger.isEnabledFor(logLevel)) {
            logTiming(getLabel());
        }
    }

    public void stopAndLogTiming() {
        stopAndLogTiming("");
    }

    private void logTiming(String message) {
        StringBuffer sb = new StringBuffer();
        sb.append("[(t)");
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd.HH:mm:ss.SSS");
        sb.append(df.format(new Date()));
        sb.append(" ");
        sb.append(logTag);
        sb.append("] ");
        sb.append(unqualifiedClassName).append(" ");
        sb.append(message);
        float ms = (float)getDurationMicro()/1000;
        sb.append(": ").append(ms).append(" ms");
        logger.log(logLevel, sb.toString());
    }

}
