/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel;

public class MdwCamelException extends Exception {

    public MdwCamelException(String msg) {
        super(msg);
    }
    
    public MdwCamelException(String msg, Throwable t) {
        super(msg, t);
    }
}
