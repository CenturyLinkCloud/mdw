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

    @Override
    public boolean equals(Object other) {
        return other != null && toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

}
