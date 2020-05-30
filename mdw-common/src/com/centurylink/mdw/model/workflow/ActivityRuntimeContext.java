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
package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.Attributes;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.util.JsonUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.sun.el.ValueExpressionLiteral;
import org.json.JSONException;
import org.json.JSONObject;

import javax.el.ValueExpression;
import java.util.HashMap;
import java.util.Map;

public class ActivityRuntimeContext extends ProcessRuntimeContext implements Jsonable {

    private Activity activity;
    public Activity getActivity() { return activity; }

    // interface extending ActivityCategory
    private String category;
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    private ActivityInstance activityInstance;
    public ActivityInstance getActivityInstance() { return activityInstance; }

    @Override
    public Long getInstanceId() {
        return getActivityInstanceId();
    }

    public Attributes getAttributes() {
        return activity.getAttributes() == null ? new Attributes() : activity.getAttributes();
    }

    public String getAttribute(String name) {
        return getAttributes().get(name);
    }

    private boolean suspendable;
    public boolean isSuspendable() { return suspendable; }

    public ActivityRuntimeContext(StandardLogger logger, Package pkg, Process process, ProcessInstance processInstance, int performanceLevel,
            boolean inService, Activity activity, String category, ActivityInstance activityInstance, boolean suspendable) {
        super(logger, pkg, process, processInstance, performanceLevel, inService);
        this.activity = activity;
        this.category = category;
        this.activityInstance = activityInstance;
        this.suspendable = suspendable;
    }

    public Long getActivityId() {
        return getActivity().getId();
    }

    public String getActivityLogicalId() {
        return getActivity().getLogicalId();
    }

    public Long getActivityInstanceId() {
        return getActivityInstance().getId();
    }

    protected String logtag() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.logtag());
        sb.append(" a");
        sb.append(this.getActivityId());
        sb.append(".");
        sb.append(this.getActivityInstanceId());
        return sb.toString();
    }

    public String getCompletionCode() {
        return getActivityInstance().getCompletionCode();
    }

    private Map<String,ValueExpression> valueExpressionMap;
    @Override
    protected Map<String,ValueExpression> getValueExpressionMap() {
        if (valueExpressionMap == null) {
            valueExpressionMap = super.getValueExpressionMap();
            valueExpressionMap.put("context", new ValueExpressionLiteral(this, Object.class));
        }
        return valueExpressionMap;
    }

    public ActivityRuntimeContext(JSONObject json) throws ServiceException {
        super(null,null, null, null, 0, false, json.has("variables") ? new HashMap<>() : null);
        String procPath = json.getString("process");
        int slash = procPath.indexOf("/");
        if (slash > 0) {
            pkg = new Package();
            pkg.setName(procPath.substring(0, slash));
            process = new Process();
            process.setName(procPath.substring(slash + 1));
            process.setPackageName(pkg.getName());
        }
        else {
            process = new Process();
            process.setName(procPath);
        }
        this.activity = new Activity(json.getJSONObject("activity"));
        if (json.has("category"))
            category = json.getString("category");
        this.activityInstance = new ActivityInstance(json.getJSONObject("activityInstance"));
        this.processInstance = new ProcessInstance(json.getJSONObject("processInstance"));
        if (json.has("variables")) {
            Map<String,String> varMap = JsonUtil.getMap(json.getJSONObject("variables"));
            for (String name : varMap.keySet()) {
                String val = varMap.get(name);
                getValues().put(name, getValueForString(name, val));
            }
        }
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("activity", getActivity().getJson());
        if (category != null)
            json.put("category", category);
        json.put("activityInstance", getActivityInstance().getJson());
        json.put("process", getPackage().getName() + "/" + getProcess().getName());
        json.put("processInstance", getProcessInstance().getJson());
        return json;
    }

    public String getJsonName() {
        return "activityRuntimeContext";
    }
}
