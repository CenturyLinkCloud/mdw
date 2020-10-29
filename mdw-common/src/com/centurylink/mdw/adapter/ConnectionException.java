package com.centurylink.mdw.adapter;

public class ConnectionException extends Exception {

    public static final int CONNECTION_DOWN = 41290;
    public static final int CONFIGURATION_WRONG = 41286;

    private int code;

    public ConnectionException(String message) {
        super(message);
        this.code = CONNECTION_DOWN;
    }

    public ConnectionException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode(){
        return code;
    }

}
