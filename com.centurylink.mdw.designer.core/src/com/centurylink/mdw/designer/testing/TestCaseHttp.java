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
