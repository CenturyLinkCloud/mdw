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
