/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.event;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.model.Response;

public class AdapterStubResponse extends Response implements Jsonable {

    private static final String JSON_NAME = "AdapterStubResponse";

    private int delay; // seconds
    public int getDelay() { return delay; }
    public void setDelay(int delay) { this.delay = delay; }

    private boolean passthrough;
    public boolean isPassthrough() { return passthrough; }
    public void setPassthrough(boolean passthrough) { this.passthrough = passthrough; }

    public AdapterStubResponse(String content) {
        super(content);
    }

    public AdapterStubResponse(JSONObject json) throws JSONException {
        super(json.getJSONObject(JSON_NAME));
        if (json.getJSONObject(JSON_NAME).has("delay"))
            this.delay = json.getJSONObject(JSON_NAME).getInt("delay");
        if (json.getJSONObject(JSON_NAME).has("passthrough"))
            this.passthrough = json.getJSONObject(JSON_NAME).getBoolean("passthrough");
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject responseJson = super.getJson();
        if (delay > 0)
            responseJson.put("delay", delay);
        if (passthrough)
            responseJson.put("passthrough", passthrough);
        json.put(JSON_NAME, responseJson);
        return json;
    }

    public String getJsonName() {
        return JSON_NAME;
    }

}
