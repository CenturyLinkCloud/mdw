/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.activity;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.utilities.JsonUtil;

public class ActivityStubResponse implements Jsonable {

    private String resultCode;
    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }

    private int sleep;
    public int getSleep() { return sleep; }
    public void setSleep(int secs) { this.sleep = secs; }

    private Map<String,String> variables;
    public Map<String,String> getVariables() { return variables; }
    public void setVariables(Map<String,String> variables) { this.variables = variables; }

    public ActivityStubResponse() {
    }

    public ActivityStubResponse(JSONObject json) throws JSONException {
        if (json.has("resultCode"))
            resultCode = json.getString("resultCode");
        if (json.has("sleep"))
            sleep = json.getInt("sleep");
        if (json.has("variables"))
            variables = JsonUtil.getMap(json.getJSONObject("variables"));
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (resultCode != null)
            json.put("resultCode", resultCode);
        if (sleep > 0)
            json.put("sleep", sleep);
        if (variables != null)
            json.put("variables", JsonUtil.getJson(variables));
        return json;
    }

    @Override
    public String getJsonName() {
        return getClass().getSimpleName();
    }
}
