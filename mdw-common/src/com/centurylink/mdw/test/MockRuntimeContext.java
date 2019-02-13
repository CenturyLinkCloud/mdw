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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.dataaccess.BaselineData;
import com.centurylink.mdw.dataaccess.file.MdwBaselineData;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;

/**
 * Mock runtime context for programmatic activity execution during unit tests.
 */
public class MockRuntimeContext extends ActivityRuntimeContext {

    private SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd-HHmmssSSS");

    private BaselineData baselineData;
    private BaselineData getBaselineData() {
        if (baselineData == null)
            baselineData = new MdwBaselineData();
        return baselineData;
    }

    protected String masterRequestId;
    @Override
    public String getMasterRequestId() {
        if (masterRequestId == null)
            masterRequestId = "Test-" + sdf.format(new Date());
        return masterRequestId;
    }

    protected Package pkg;
    @Override
    public Package getPackage() {
        if (pkg == null) {
            pkg = new Package();
            pkg.setName("com.centurylink.mdw.test");
            pkg.setVersion(0);
            pkg.setId(0L);
        }
        return pkg;
    }

    protected Process process;
    @Override
    public Process getProcess() {
        if (process == null) {
            process = new Process();
            process.setName("TestProcess");
            process.setVersion(0);
            process.setId(0L);
            List<Variable> processVars = new ArrayList<Variable>();
            Map<String,Object> vars = getVariables();
            if (vars != null) {
                for (String varName : vars.keySet()) {
                    processVars.add(new Variable(varName, getBaselineData().getVariableType(vars.get(varName))));
                }
            }
            process.setVariables(processVars);
        }
        return process;
    }

    protected ProcessInstance processInstance;
    @Override
    public ProcessInstance getProcessInstance() {
        if (processInstance == null) {
            processInstance = new ProcessInstance();
        }
        return processInstance;
    }

    private String activityName;
    protected Activity activity;
    @Override
    public Activity getActivity() {
        if (activity == null) {
            activity = new Activity();
            activity.setId(0L);
            activity.setName(activityName);
            List<Attribute> attrs = new ArrayList<Attribute>();
            for (String attrName : getAttributes().keySet())
                attrs.add(new Attribute(attrName, getAttributes().get(attrName)));
        }
        return activity;
    }

    protected ActivityInstance activityInstance;
    public ActivityInstance getActivityInstance() {
        if (activityInstance == null)
            activityInstance = new ActivityInstance();
        return activityInstance;
    }

    protected Map<String,Object> variables;
    @Override
    public Map<String,Object> getVariables() {
        if (variables == null)
            variables = new HashMap<>();
        return variables;
    }

    protected Map<String,String> docRefs;
    public Map<String,String> getDocRefs() {
        if (docRefs == null)
            docRefs = new HashMap<>();
        return docRefs;
    }

    protected Map<String,String> attributes;
    @Override
    public Map<String,String> getAttributes() {
        if (attributes == null)
            attributes = new HashMap<>();
        return attributes;
    }

    protected Map<String,String> properties;
    public Map<String,String> getProperties() {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties;
    }
    public Map<String,String> getProps() {
        return getProperties();
    }

    public MockRuntimeContext(String activityName) {
        this(activityName, null);
    }

    public MockRuntimeContext(String activityName, BaselineData baselineData) {
        super(null, null, null, 0, null, null);
        this.activityName = activityName;
        this.baselineData = baselineData;
        PropertyManager.initializeMockPropertyManager();
    }

    public void setAdapterStubbedResponse(String response) {
        getAttributes().put(WorkAttributeConstant.SIMULATION_STUB_MODE, "On");
        getAttributes().put(WorkAttributeConstant.SIMULATION_RESPONSE + "1", ",1," + response);
    }

    @Override
    public Long getProcessId() {
        return 0L;
    }

    @Override
    public Long getProcessInstanceId() {
        return 0L;
    }

    @Override
    public String getMdwVersion() {
        return "6.1";
    }
}
