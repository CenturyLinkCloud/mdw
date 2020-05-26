package com.centurylink.mdw.request;

public class RequestHandlerException extends Exception {

    public RequestHandlerException(String message){
        super(message);
    }

    public RequestHandlerException(String message, Throwable cause){
        super(message, cause);
    }
}
