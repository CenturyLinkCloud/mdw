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
