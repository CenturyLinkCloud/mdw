package com.centurylink.mdw.util;

import com.centurylink.mdw.common.MdwException;

public class ServiceLocatorException extends MdwException {

    public ServiceLocatorException(String message){
        super(message);
    }

    public ServiceLocatorException(int code, String message){
        super(code, message);

    }

    public ServiceLocatorException(int code, String message, Throwable cause){
        super(code, message, cause);

    }
}
