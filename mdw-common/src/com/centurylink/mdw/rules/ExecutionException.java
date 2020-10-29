package com.centurylink.mdw.rules;

import com.centurylink.mdw.common.MdwException;

public class ExecutionException extends MdwException {

    public ExecutionException(String message) {
        super(message);
    }

    public ExecutionException(String message, Throwable t){
        super(message, t);
    }

    public ExecutionException(int code, String message, Throwable t){
        super(code, message, t);
    }
}
