package com.centurylink.mdw.connector.adapter;

/**
 * @deprecated
 * use {@link com.centurylink.mdw.adapter.AdapterException}
 */
@Deprecated
public class AdapterException extends com.centurylink.mdw.adapter.AdapterException {

    @Deprecated
    public AdapterException(String message) {
        super(message);
    }
    @Deprecated
    public AdapterException(String message, boolean retryable) {
        super(message, retryable);
    }
    @Deprecated
    public AdapterException(int code, String message) {
        super(code, message);
    }
    @Deprecated
    public AdapterException(int code, String message, boolean retryable) {
        super(code, message, retryable);
    }

    public AdapterException(String message, Throwable cause) {
        super(message, cause);
    }

    public AdapterException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public AdapterException(int code, String message, Throwable cause, boolean retryable) {
        super(code, message, cause, retryable);
    }
}
