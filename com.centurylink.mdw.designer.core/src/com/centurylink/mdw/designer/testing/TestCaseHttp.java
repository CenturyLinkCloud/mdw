/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

public class TestCaseHttp {

    private String uri;
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    private String method;
    public String getMethod() { return method; }
    public void setMethod(String m) { this.method = m; }

    private TestCaseMessage message;
    public TestCaseMessage getMessage() { return message; }
    public void setMessage(TestCaseMessage m) { this.message = m; }

    public TestCaseHttp(String uri) {
        this.uri = uri;
    }
}
