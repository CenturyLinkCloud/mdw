package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.common.service.ServiceException;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class ActivityStubRequest implements Jsonable {

    public static final String JSON_NAME = "ActivityStubRequest";

    private ActivityRuntimeContext runtimeContext;
    public ActivityRuntimeContext getRuntimeContext() { return runtimeContext; }
    public void setRuntimeContext(ActivityRuntimeContext runtimeContext) { this.runtimeContext = runtimeContext; }

    public ActivityStubRequest(ActivityRuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }

    public ActivityStubRequest(JSONObject json) throws JSONException {
        JSONObject stubRequestJson = json.getJSONObject(JSON_NAME);
        if (stubRequestJson.has("runtimeContext")) {
            try {
                runtimeContext = new ActivityRuntimeContext(stubRequestJson.getJSONObject("runtimeContext"));
            } catch (ServiceException ex) {
                throw new JSONException(ex);
            }
        }
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        JSONObject stubRequestJson = create();
        if (runtimeContext != null)
            stubRequestJson.put("runtimeContext", runtimeContext.getJson());
        json.put(JSON_NAME, stubRequestJson);
        return json;
    }

    public String getJsonName() {
        return JSON_NAME;
    }
}
