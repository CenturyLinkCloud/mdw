/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.test;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

public class TestExecConfig implements Jsonable {

    private int threadCount = 5;
    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }

    public TestExecConfig() {
        // default options
    }

    public TestExecConfig(JSONObject json) throws JSONException {
        if (json.has("threadCount"))
            this.threadCount = json.getInt("threadCount");
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("threadCount", threadCount);


        return json;
    }

    public String getJsonName() {
        return "testExecConfig";
    }
}
