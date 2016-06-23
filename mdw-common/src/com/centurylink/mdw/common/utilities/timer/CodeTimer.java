/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities.timer;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;

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
