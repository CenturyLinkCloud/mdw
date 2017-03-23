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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.model.value.process.ProcessVO;

import groovy.lang.GroovyObjectSupport;

public class TestCaseProcess extends GroovyObjectSupport implements Verifiable {
    private ProcessVO processVo;
    public ProcessVO getProcessVo() { return processVo; }

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

    public TestCaseProcess(ProcessVO processVo) {
        this.processVo = processVo;
    }

    public String getLabel() {
        return processVo.getLabel() + (activityLogicalId == null ? "" : ":" + activityLogicalId);
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

    private TestCaseAsset expectedResults;
    public TestCaseAsset getExpectedResults() { return expectedResults; }
    public void setExpectedResults(TestCaseAsset expectedResults) { this.expectedResults = expectedResults; }

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
