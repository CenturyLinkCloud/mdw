package com.centurylink.mdw.observer;

import com.centurylink.mdw.common.MdwException;

public class ObserverException extends MdwException {

    public ObserverException(String message){
        super(message);
    }

    public ObserverException(int code, String message){
        super(code, message);

    }

    public ObserverException(String message, Throwable cause){
        super(message, cause);
    }

    public ObserverException(int code, String message, Throwable cause){
        super(code, message, cause);

    }

}
