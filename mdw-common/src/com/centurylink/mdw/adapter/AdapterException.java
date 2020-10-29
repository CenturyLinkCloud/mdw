package com.centurylink.mdw.adapter;

import com.centurylink.mdw.common.MdwException;

public class AdapterException extends MdwException {

    public static final int EXCEED_MAXTRIES = 41282;

    private boolean retryable;

    public AdapterException(String message) {
        super(message);
    }

    public AdapterException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public AdapterException(int code, String message) {
        super(code, message);

    }

    public AdapterException(int code, String message, boolean retryable) {
        super(code, message);
        this.retryable = retryable;
    }

    public AdapterException(String message, Throwable cause) {
        super(message, cause);

    }

    public AdapterException(int code, String message, Throwable cause) {
        super(code, message, cause);

    }

    public AdapterException(int code, String message, Throwable cause, boolean retryable) {
        super(code, message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }
}
