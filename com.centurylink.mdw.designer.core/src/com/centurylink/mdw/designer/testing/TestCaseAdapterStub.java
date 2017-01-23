/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

import groovy.lang.Closure;

public class TestCaseAdapterStub {

    private Closure<Boolean> matcher;
    public Closure<Boolean> getMatcher() { return matcher; }
    public void setMatcher(Closure<Boolean> matcher) { this.matcher = matcher; }

    private Closure<String> responder;
    public Closure<String> getResponder() { return responder; }
    public void setResponder(Closure<String> responder) { this.responder = responder; }

    /**
     * This response may be hardcoded or contain placeholders or be empty altogether.
     * It's made available through the "this" argument to the responder closure.
     */
    private String response;
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    /**
     * Endpoint true means "stub endpoint" instead of "stub adapter".  See docs.
     */
    private boolean endpoint;
    public boolean isEndpoint() { return endpoint; }
    public void setEndpoint(boolean endpoint) { this.endpoint = endpoint; }

    /**
     * Represents a server-side sleep.  Support either "sleep" or "delay" syntax.
     */
    private int delay;
    public int getDelay() { return delay; }
    public void setDelay(int delay) { this.delay = delay; }
    private int sleep;
    public int getSleep() { return sleep; }
    public void setSleep(int sleep) { this.sleep = sleep; }

    private int statusCode;
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int code) { this.statusCode = code; }

    private String statusMessage;
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String message) { this.statusMessage = message; }

    public TestCaseAdapterStub(Closure<Boolean> matcher, Closure<String> responder) {
        this.matcher = matcher;
        this.responder = responder;
    }


    public String toString() {
        return response;
    }

}
