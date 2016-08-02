/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

import java.util.Map;

public class TestCaseResponse implements Verifiable {
    private String expected;
    public String getExpected() { return this.expected; }
    public void setExpected(String expected) { this.expected = expected; }

    private String actual;
    public String getActual() { return this.actual; }
    public void setActual(String actual) { this.actual = actual; }

    public String getContent() {
        return actual;
    }

    public TestCaseResponse() {
    }

    public TestCaseResponse(TestCaseResponse cloneFrom) {
        this.expected = cloneFrom.expected;
        this.actual = cloneFrom.actual;
        this.success = cloneFrom.success;
    }

    public TestCaseResponse file(String name) throws TestException {
        return null; // placeholder for command chaining
    }

    public TestCaseResponse asset(String name) throws TestException {
        return null; // placeholder for command chaining
    }

    private boolean success;
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    private Map<String,String> headers;
    public Map<String,String> getHeaders() { return headers; }
    public void setHeaders(Map<String,String> headers) { this.headers = headers; }

    // http response code
    private int code;
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }


}
