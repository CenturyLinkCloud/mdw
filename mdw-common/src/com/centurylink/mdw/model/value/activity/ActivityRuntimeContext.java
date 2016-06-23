/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.activity;

import java.util.HashMap;
import java.util.Map;

import javax.el.ValueExpression;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessRuntimeContext;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.sun.el.ValueExpressionLiteral;

public class ActivityRuntimeContext extends ProcessRuntimeContext implements Jsonable {

    private ActivityVO activityVO;
    public ActivityVO getActivity() { return activityVO; }

    private ActivityInstanceVO activityInstanceVO;
    public ActivityInstanceVO getActivityInstance() { return activityInstanceVO; }

    private Map<String,String> attributes;
    public Map<String,String> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<String,String>();
            for (AttributeVO attribute : activityVO.getAttributes()) {
                attributes.put(attribute.getAttributeName(), attribute.getAttributeValue());
            }
        }
        return attributes;
    }

    public ActivityRuntimeContext(PackageVO packageVO, ProcessVO processVO, ProcessInstanceVO processInstanceVO, ActivityVO activityVO, ActivityInstanceVO activityInstanceVO) {
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
        super(null, null, null);
        JSONObject contextJson = json.getJSONObject(getJsonName());
        String procPath = contextJson.getString("process");
        int slash = procPath.indexOf("/");
        if (slash > 0) {
            packageVO = new PackageVO();
            packageVO.setName(procPath.substring(0, slash));
            processVO = new ProcessVO();
            processVO.setName(procPath.substring(slash + 1));
        }
        else {
            processVO = new ProcessVO();
            processVO.setName(procPath);
        }
        this.activityVO = new ActivityVO(contextJson.getJSONObject("activity"));
        this.activityInstanceVO = new ActivityInstanceVO(contextJson.getJSONObject("activityInstance"));
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject contextJson = new JSONObject();
        contextJson.put("activity", getActivity().getJson());
        contextJson.put("activityInstance", getActivityInstance().getJson());
        contextJson.put("process", getPackage().getName() + "/" + getProcess().getName());
        json.put(getJsonName(), contextJson);
        return json;
    }

    public String getJsonName() {
        return "ActivityRuntimeContext";
    }
}
