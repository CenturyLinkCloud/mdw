/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.requests;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.InstanceCount;
import com.centurylink.mdw.common.service.Jsonable;

public class RequestCount implements InstanceCount, Jsonable {

    private long count = -1;
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    private String type;
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public RequestCount(long count) {
        this.count = count;
    }

    public RequestCount(JSONObject json) throws JSONException {
        count = json.getLong("count");
        if (json.has("type"))
            type = json.getString("type");
    }

    public String getJsonName() {
        return "requestCount";
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("count", count);
        if (type != null)
            json.put("type", type);
        return json;
    }
}
