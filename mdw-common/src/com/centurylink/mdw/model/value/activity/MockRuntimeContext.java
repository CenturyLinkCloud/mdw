/**
 * Copyright (c) 2013 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.dataaccess.BaselineData;
import com.centurylink.mdw.dataaccess.file.MdwBaselineData;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;

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

    protected PackageVO pkg;
    @Override
    public PackageVO getPackage() {
        if (pkg == null) {
            pkg = new PackageVO();
            pkg.setName("com.centurylink.mdw.test");
            pkg.setVersion(0);
            pkg.setId(0L);
        }
        return pkg;
    }

    protected ProcessVO process;
    @Override
    public ProcessVO getProcess() {
        if (process == null) {
            process = new ProcessVO();
            process.setName("TestProcess");
            process.setVersion(0);
            process.setId(0L);
            List<VariableVO> processVars = new ArrayList<VariableVO>();
            Map<String,Object> vars = getVariables();
            if (vars != null) {
                for (String varName : vars.keySet()) {
                    processVars.add(new VariableVO(varName, getBaselineData().getVariableType(vars.get(varName))));
                }
            }
            process.setVariables(processVars);
        }
        return process;
    }

    protected ProcessInstanceVO processInstance;
    @Override
    public ProcessInstanceVO getProcessInstance() {
        if (processInstance == null) {
            processInstance = new ProcessInstanceVO();
        }
        return processInstance;
    }

    private String activityName;
    protected ActivityVO activity;
    @Override
    public ActivityVO getActivity() {
        if (activity == null) {
            activity = new ActivityVO();
            activity.setActivityId(0L);
            activity.setActivityName(activityName);
            List<AttributeVO> attrs = new ArrayList<AttributeVO>();
            for (String attrName : getAttributes().keySet())
                attrs.add(new AttributeVO(attrName, getAttributes().get(attrName)));
        }
        return activity;
    }

    protected ActivityInstanceVO activityInstance;
    public ActivityInstanceVO getActivityInstance() {
        if (activityInstance == null)
            activityInstance = new ActivityInstanceVO();
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
