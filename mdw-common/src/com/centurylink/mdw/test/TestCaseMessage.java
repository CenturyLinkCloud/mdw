/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.test;

import java.util.Map;

public class TestCaseMessage {
    private String protocol = "REST";
    public String getProtocol() { return this.protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    private String payload;
    public String getPayload() { return this.payload; }
    public void setPayload(String payload) { this.payload = payload; }

    private String user;
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String password;
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    private Map<String,String> headers;
    public Map<String,String> getHeaders() { return headers; }
    public void setHeaders(Map<String,String> headers) { this.headers = headers; }

    public TestCaseMessage() {
        // defaults to REST
    }

    /**
     * Process launch message
     */
    public TestCaseMessage(String masterRequestId, TestCaseProcess process) {
        payload =
           "<ser:ActionRequest xmlns:ser='http://mdw.qwest.com/services'>\n"
         + "  <Action Name='RegressionTest'>\n"
         + "    <Parameter name='MasterRequestId'>" + masterRequestId + "</Parameter>\n"
         + "    <Parameter name='ProcessName'>" + process.getProcess().getName() + "</Parameter>\n";

        if (process.getVariables() != null) {
            for (String name : process.getVariables().keySet()) {
                Object val = process.getVariables().get(name);
                if (val != null)
                    payload += "    <Parameter name='" + name + "'>" + val + "</Parameter>\n";
            }
        }

        payload +=
           "  </Action>\n"
         + "</ser:ActionRequest>";
    }

    public TestCaseMessage(String protocol) {
        this.protocol = protocol;
    }
}
