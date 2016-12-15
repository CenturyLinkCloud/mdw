/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */

package com.centurylink.mdw.tests.workflow;

/**
 * Dynamic Java workflow asset.
 */
public class TimerBean implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private int timerDelaySeconds;
    /**
     * Does a calculation simulating dueDate calc.
     */
    public int getTimerDelaySeconds() {
        timerDelaySeconds -= 100;
        return timerDelaySeconds;
    }
    public void setTimerDelaySeconds(int delaySeconds) {
        this.timerDelaySeconds = delaySeconds;
    }

    public String toString() {
        return "{ timerDelaySeconds: " + timerDelaySeconds + " }";
    }

    public boolean equals(Object other) {
        return toString().equals(other.toString());
    }
}
