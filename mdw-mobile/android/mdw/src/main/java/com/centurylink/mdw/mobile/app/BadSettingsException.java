package com.centurylink.mdw.mobile.app;

public class BadSettingsException  extends RuntimeException {
    public BadSettingsException(String msg) {
        super(msg);
    }

    public BadSettingsException(String msg, Throwable cause) {
        super(msg, cause);
    }
}