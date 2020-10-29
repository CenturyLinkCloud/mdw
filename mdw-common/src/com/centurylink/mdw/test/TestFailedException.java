package com.centurylink.mdw.test;

public class TestFailedException extends TestException {

    public TestFailedException(String msg) {
        super(msg);
    }

   public TestFailedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
