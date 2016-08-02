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

    private int connectTimeout;
    public int getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(int ms) { this.connectTimeout = ms; }

    private int readTimeout;
    public int getReadTimeout() { return readTimeout; }
    public void setReadTimeout(int ms) { this.readTimeout = ms; }

    private TestCaseMessage message;
    public TestCaseMessage getMessage() { return message; }
    public void setMessage(TestCaseMessage m) { this.message = m; }

    public TestCaseHttp(String uri) {
        this.uri = uri;
    }
}
