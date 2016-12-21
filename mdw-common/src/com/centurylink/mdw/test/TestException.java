/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.test;

public class TestException extends Exception {

    public TestException(String msg) {
        super(msg);
    }

   public TestException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
