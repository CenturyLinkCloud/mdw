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
