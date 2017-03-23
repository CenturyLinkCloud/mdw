/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.designer.testing;

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
         + "    <Parameter name='ProcessName'>" + process.getProcessVo().getName() + "</Parameter>\n";

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
