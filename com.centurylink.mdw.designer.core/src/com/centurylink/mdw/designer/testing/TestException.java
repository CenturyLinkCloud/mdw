/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

public class TestException extends Exception {

    private TestFileLine cmd;

    public TestException(String msg) {
        super(msg);
    }

    public TestException(TestFileLine cmd, String msg) {
        super(msg);
        this.cmd = cmd;
    }

   public TestException(String msg, Throwable cause) {
        super(msg, cause);
    }

    TestException(TestFileLine cmd, String msg, Throwable cause) {
        super(msg, cause);
        this.cmd = cmd;
    }

    @Override
    public String getMessage() {
        if (cmd == null)
            return super.getMessage();
        else
            return "Command "+cmd.getCommand() + " at line "+cmd.getLineNumber() + ": " + super.getMessage();
    }

}
