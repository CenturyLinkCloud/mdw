/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.test;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

public class TestExecConfig implements Jsonable {

    private boolean verbose;
    public boolean isVerbose() { return verbose; }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }

    private boolean loadTest;
    public boolean isLoadTest() { return loadTest; }
    public void setLoadTest(boolean loadTest) { this.loadTest = loadTest; }

    private boolean createReplace;
    public boolean isCreateReplace() { return createReplace; }
    public void setCreateReplace(boolean createReplace) { this.createReplace = createReplace; }

    private boolean stubbing;
    public boolean isStubbing() { return stubbing; }
    public void setStubbing(boolean stubbing) { this.stubbing = stubbing; }

    private int interval; // seconds
    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }

    public TestExecConfig() {
        // default options
    }

    public TestExecConfig(JSONObject json) throws JSONException {
        if (json.has("verbose"))
            this.verbose = json.getBoolean("verbose");
        if (json.has("loadTest"))
            this.loadTest = json.getBoolean("loadTest");
        if (json.has("createReplace"))
            this.createReplace = json.getBoolean("createReplace");
        if (json.has("stubbing"))
            this.stubbing = json.getBoolean("stubbing");
        if (json.has("interval"))
            this.interval = json.getInt("interval");
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (verbose)
            json.put("verbose", verbose);
        if (loadTest)
            json.put("loadTest", loadTest);
        if (createReplace)
            json.put("createReplace", createReplace);
        if (stubbing)
            json.put("stubbing", stubbing);
        if (interval > 0)
            json.put("interval", interval);

        return json;
    }

    public String getJsonName() {
        return "testExecConfig";
    }
}
