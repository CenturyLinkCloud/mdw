package com.centurylink.mdw.connector.adapter;

/**
 * @deprecated
 * use {@link com.centurylink.mdw.adapter.ConnectionException}
 */
@Deprecated
public class ConnectionException extends com.centurylink.mdw.adapter.ConnectionException {
    @Deprecated
    public ConnectionException(String message) {
        super(message);
    }
    @Deprecated
    public ConnectionException(int code, String message) {
        super(code, message);
    }
    @Deprecated
    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
    @Deprecated
    public ConnectionException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
