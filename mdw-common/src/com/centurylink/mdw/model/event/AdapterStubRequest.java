/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.event;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

public class AdapterStubRequest implements Jsonable {

    public static final String JSON_NAME = "AdapterStubRequest";

    private String masterRequestId;
    public String getMasterRequestId() { return masterRequestId; }
    public void setMasterRequestId(String masterRequestId) { this.masterRequestId = masterRequestId; }

    private String content;
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public AdapterStubRequest(String masterRequestId, String content) {
        this.masterRequestId = masterRequestId;
        this.content = content;
    }

    public AdapterStubRequest(JSONObject json) throws JSONException {
        JSONObject requestJson = json.getJSONObject(getJsonName());
        this.masterRequestId = requestJson.getString("masterRequestId");
        this.content = requestJson.getString("content");
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject requestJson = new JSONObject();
        requestJson.put("masterRequestId", masterRequestId);
        requestJson.put("content", content);
        json.put(getJsonName(), requestJson);
        return json;
    }

    public String getJsonName() {
        return JSON_NAME;
    }

}
