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
package com.centurylink.mdw.test;

import java.util.Map;

public class ApiRequest {

    private TestCase testCase;
    public TestCase getTestCase() { return testCase; }

    public ApiRequest(TestCase testCase) {
        this.testCase = testCase;
    }

    private String caseName;
    public String getCaseName() { return caseName; }
    public void setCaseName(String caseName) { this.caseName = caseName; }

    private String environment;
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    private Map<String,Object> values;
    public Map<String,Object> getValues() { return values; }
    public void setValues(Map<String,Object> values) {
        this.values = values;
    }
}
