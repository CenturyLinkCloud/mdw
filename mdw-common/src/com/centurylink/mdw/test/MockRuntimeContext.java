/**
 * Copyright (c) 2013 CenturyLink, Inc. All Rights Reserved.
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
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.model.workflow.Activity;
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
            activity.setActivityId(0L);
            activity.setActivityName(activityName);
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
            variables = new HashMap<String,Object>();
        return variables;
    }

    protected Map<String,String> attributes;
    @Override
    public Map<String,String> getAttributes() {
        if (attributes == null)
            attributes = new HashMap<String,String>();
        return attributes;
    }

    protected Map<String,String> properties;
    public Map<String,String> getProperties() {
        if (properties == null) {
            properties = new HashMap<String,String>();
        }
        return properties;
    }

    public MockRuntimeContext(String activityName) {
        this(activityName, null);
    }

    public MockRuntimeContext(String activityName, BaselineData baselineData) {
        super(null, null, null, null, null);
        this.activityName = activityName;
        this.baselineData = baselineData;
        PropertyManager.initializeUnitTestPropertyManager();
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
        return "6.0";
    }
}
