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

import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class CodeTimer {

    private String label;
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    private String note;
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    private long startNano;
    private long stopNano;
    private boolean running;

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public CodeTimer(String label) {
        this.label = label;
    }

    public CodeTimer(boolean start) {
        if (start)
            start();
    }

    public CodeTimer(String label, boolean start) {
        this.label = label;
        if (start)
          start();
    }

    public void start() {
        startNano = System.nanoTime();
        running = true;
    }

    public void stop() {
        stop(null);
        running = false;
    }

    public void stop(String note) {
        stopNano = System.nanoTime();
        this.note = note;
        running = false;
    }

    /**
     * Timer duration in milliseconds.
     */
    public long getDuration() {
        return getDurationMicro() / 1000;
    }

    /**
     * Timer duration in microseconds.
     */
    public long getDurationMicro() {
        if (startNano != 0) {
            if (running)
                return ((long)(System.nanoTime() - startNano)) / 1000;
            else if (stopNano != startNano)
                return ((long)(stopNano - startNano)) / 1000;
        }
        return 0;
    }

    public void stopAndLogTiming(String note) {
        stop(note);
        if (logger.isMdwDebugEnabled())
            logger.mdwDebug(toString());
    }

    public void logTimingAndContinue(String note) {
        this.note = note;
        if (logger.isMdwDebugEnabled())
            logger.mdwDebug(toString());
    }

    public String toString() {
        StringBuffer stringValue = new StringBuffer("CodeTimer: ");
        if (label != null)
          stringValue.append("[").append(label).append("] ");
        if (note != null)
          stringValue.append(note).append(" = ");
        stringValue.append(getDuration()).append(" ms");
        return stringValue.toString();
    }
}
