package com.centurylink.mdw.camel;

public class MdwCamelException extends Exception {

    public MdwCamelException(String msg) {
        super(msg);
    }

    public MdwCamelException(String msg, Throwable t) {
        super(msg, t);
    }
}
