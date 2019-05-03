package com.centurylink.mdw.model.monitor;

@FunctionalInterface
public interface JobCompletionCallback {
    void onComplete(Integer status);
}
