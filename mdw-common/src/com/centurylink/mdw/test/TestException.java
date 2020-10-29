package com.centurylink.mdw.test;

public class TestException extends RuntimeException {

    public TestException(String msg) {
        super(msg);
    }

   public TestException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
