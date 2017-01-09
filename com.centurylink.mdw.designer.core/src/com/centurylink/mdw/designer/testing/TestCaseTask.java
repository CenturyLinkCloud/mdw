/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

import java.util.HashMap;
import java.util.Map;

/**
 * Manual task for automated tests.
 * TODO: set variable values while performing action.
 */
public class TestCaseTask {

    private Long id; // instanceId
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String outcome;
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    private Map<String,Object> variables;
    public Map<String,Object> getVariables() { return variables; }
    public void setVariables(Map<String,Object> variables) { this.variables = variables; }

    public Map<String,String> getParams() {
        Map<String, String> params = new HashMap<String, String>();
        if (variables != null) {
            for (String name : variables.keySet()) {
                Object value = variables.get(name);
                if (value != null)
                    params.put(name, value.toString()); // TODO full inflation esp. for docs
            }
        }
        return params;
    }

    public TestCaseTask(String name) {
        this.name = name;
    }

}
