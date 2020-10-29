package com.centurylink.mdw.cli;

public interface ProgressMonitor {

    void message(String msg);

    /**
     * Must be idempotent.
     */
    void progress(int percent);

    default boolean isCanceled() { return false; }
    default boolean isSupportsMessage() { return false; }
}
