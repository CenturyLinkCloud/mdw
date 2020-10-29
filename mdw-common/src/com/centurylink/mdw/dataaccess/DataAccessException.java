package com.centurylink.mdw.dataaccess;

import com.centurylink.mdw.common.MdwException;

public class DataAccessException extends MdwException {

    public static final int INTEGRITY_CONSTRAINT_VIOLATION = 23000;

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(int code, String message) {
        super(code, message);
    }

    public DataAccessException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

}
