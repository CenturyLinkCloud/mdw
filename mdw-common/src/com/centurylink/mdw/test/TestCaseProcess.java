/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.workflow.Process;

import groovy.lang.GroovyObjectSupport;

public class TestCaseProcess extends GroovyObjectSupport implements Verifiable {
    private Process process;
    public Process getProcess() { return process; }

    private Map<String,Object> variables;
    public Map<String,Object> getVariables() { return variables; }
    public void setVariables(Map<String,Object> variables) { this.variables = variables; }

    private List<TestCaseActivityStub> activityStubs;
    public List<TestCaseActivityStub> getActivityStubs() { return activityStubs; }
    public void setActivityStubs(List<TestCaseActivityStub> stubs) { this.activityStubs = stubs; }

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

    public TestCaseProcess(Process processVo) {
        this.process = processVo;
    }

    public String getLabel() {
        return process.getLabel() + (activityLogicalId == null ? "" : ":" + activityLogicalId);
    }

    private boolean success;
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    /**
     * The following properties are for "process wait" commands.
     */
    private int timeout = 60;
    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }

    private String activityLogicalId;
    public String getActivityLogicalId() { return activityLogicalId; }
    public void setActivityLogicalId(String logicalId) { this.activityLogicalId = logicalId; }

    private String status;
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    private Asset expectedResults;
    public Asset getExpectedResults() { return expectedResults; }
    public void setExpectedResults(Asset expectedResults) { this.expectedResults = expectedResults; }

    private String results;
    public String getResults() { return results; }
    public void setResults(String results) { this.results = results; }

    /**
     * Activity order in results.
     */
    private boolean resultsById;
    public boolean isResultsById() { return resultsById; }
    public void setResultsById(boolean byId) { this.resultsById = byId; }
}
