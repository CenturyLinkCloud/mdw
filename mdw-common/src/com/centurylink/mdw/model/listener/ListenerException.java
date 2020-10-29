package com.centurylink.mdw.model.listener;

import java.io.Serializable;

import com.centurylink.mdw.common.MdwException;

public class ListenerException extends MdwException implements Serializable {

    public ListenerException(String message){
        super(message);
    }

    public ListenerException(int code, String message){
        super(code, message);

    }

    public ListenerException(int code, String message, Throwable cause){
        super(code, message, cause);

    }
}