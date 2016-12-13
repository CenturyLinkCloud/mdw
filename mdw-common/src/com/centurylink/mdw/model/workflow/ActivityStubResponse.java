/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.workflow;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.util.JsonUtil;

public class ActivityStubResponse extends Response implements Jsonable {

    private String resultCode;
    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }

    private int delay;
    public int getDelay() { return delay; }
    public void setDelay(int secs) { this.delay = secs; }

    private boolean passthrough;
    public boolean isPassthrough() { return passthrough; }
    public void setPassthrough(boolean passthrough) { this.passthrough = passthrough; }

    private Map<String,String> variables;
    public Map<String,String> getVariables() { return variables; }
    public void setVariables(Map<String,String> variables) { this.variables = variables; }

    public ActivityStubResponse() {
        super((String)null);
    }

    public ActivityStubResponse(JSONObject json) throws JSONException {
        super(json);
        if (json.has("resultCode"))
            resultCode = json.getString("resultCode");
        if (json.has("delay"))
            delay = json.getInt("delay");
        else if (json.has("sleep"))
            delay = json.getInt("sleep");
        if (json.has("variables"))
            variables = JsonUtil.getMap(json.getJSONObject("variables"));
        if (json.has("passthrough"))
            passthrough = json.getBoolean("passthrough");
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (resultCode != null)
            json.put("resultCode", resultCode);
        if (delay > 0)
            json.put("delay", delay);
        if (variables != null)
            json.put("variables", JsonUtil.getJson(variables));
        if (passthrough)
            json.put("passthrough", passthrough);
        return json;
    }

    @Override
    public String getJsonName() {
        return getClass().getSimpleName();
    }
}
