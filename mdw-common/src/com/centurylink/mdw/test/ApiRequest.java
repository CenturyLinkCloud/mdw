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

import java.util.HashMap;
import java.util.Map;

public class ApiRequest {

    private TestCase testCase;
    public TestCase getTestCase() { return testCase; }

    public ApiRequest(TestCase testCase) {
        this.testCase = testCase;
    }

    public ApiRequest(TestCase testCase, Map<String,String> options) {
        this.testCase = testCase;
        this.options = options;
    }

    /**
     * Runner options (eg: environment, capture)
     */
    private Map<String,String> options;
    public Map<String,String> getOptions() { return options; }
    public void setOptions(Map<String,String> options) {
        this.options = options;
    }

    /**
     * Runtime values.
     */
    private Map<String,Object> values;
    public Map<String,Object> getValues() { return values; }
    public void setValues(Map<String,Object> values) {
        this.values = values;
    }

    /**
     * Creates a clone with overridden values.
     * Options are not deeply cloned.
     */
    public ApiRequest leftShift(Map<String,Object> newValues) {
        ApiRequest newOne = new ApiRequest(testCase, options);
        if (values != null) {
            newOne.values = new HashMap<>();
            for (String key : values.keySet())
                newOne.values.put(key, values.get(key));
            for (String key : newValues.keySet())
                newOne.values.put(key, newValues.get(key));
        }
        return newOne;
    }
}
