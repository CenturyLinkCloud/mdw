/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.workflow;

import java.util.HashMap;
import java.util.Map;

import javax.el.ValueExpression;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.util.JsonUtil;
import com.sun.el.ValueExpressionLiteral;

public class ActivityRuntimeContext extends ProcessRuntimeContext implements Jsonable {

    private Activity activityVO;
    public Activity getActivity() { return activityVO; }

    private ActivityInstance activityInstanceVO;
    public ActivityInstance getActivityInstance() { return activityInstanceVO; }

    private Map<String,String> attributes;
    public Map<String,String> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<String,String>();
            for (Attribute attribute : activityVO.getAttributes()) {
                attributes.put(attribute.getAttributeName(), attribute.getAttributeValue());
            }
        }
        return attributes;
    }

    public ActivityRuntimeContext(Package packageVO, Process processVO, ProcessInstance processInstanceVO, Activity activityVO, ActivityInstance activityInstanceVO) {
        super(packageVO, processVO, processInstanceVO);
        this.activityVO = activityVO;
        this.activityInstanceVO = activityInstanceVO;
    }

    public Long getActivityId() {
        return getActivity().getActivityId();
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

    public ActivityRuntimeContext(JSONObject json) throws JSONException {
        super(null, null, null, json.has("variables") ? new HashMap<>() : null);
        String procPath = json.getString("process");
        int slash = procPath.indexOf("/");
        if (slash > 0) {
            packageVO = new Package();
            packageVO.setName(procPath.substring(0, slash));
            processVO = new Process();
            processVO.setName(procPath.substring(slash + 1));
            processVO.setPackageName(packageVO.getName());
        }
        else {
            processVO = new Process();
            processVO.setName(procPath);
        }
        this.activityVO = new Activity(json.getJSONObject("activity"));
        this.activityInstanceVO = new ActivityInstance(json.getJSONObject("activityInstance"));
        this.processInstanceVO = new ProcessInstance(json.getJSONObject("processInstance"));
        if (json.has("variables")) {
            Map<String,String> varMap = JsonUtil.getMap(json.getJSONObject("variables"));
            for (String name : varMap.keySet()) {
                String val = varMap.get(name);
                getVariables().put(name, getValueForString(name, val));
            }
        }
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("activity", getActivity().getJson());
        json.put("activityInstance", getActivityInstance().getJson());
        json.put("process", getPackage().getName() + "/" + getProcess().getName());
        json.put("processInstance", getProcessInstance().getJson());
        return json;
    }

    public String getJsonName() {
        return "activityRuntimeContext";
    }
}
