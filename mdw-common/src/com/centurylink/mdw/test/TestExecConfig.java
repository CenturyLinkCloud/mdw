/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.test;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

public class TestExecConfig implements Jsonable {

    private int threads = 5; // thread pool size
    public int getThreads() { return threads; }
    public void setThreads(int threads) { this.threads = threads; }

    private int interval = 2; // seconds
    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }

    private boolean stubbing;
    public boolean isStubbing() { return stubbing; }
    public void setStubbing(boolean stubbing) { this.stubbing = stubbing; }

    private boolean loadTest; // ignored presently
    public boolean isLoadTest() { return loadTest; }
    public void setLoadTest(boolean loadTest) { this.loadTest = loadTest; }

    private boolean verbose = true;
    public boolean isVerbose() { return verbose; }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }

    private boolean createReplace;
    public boolean isCreateReplace() { return createReplace; }
    public void setCreateReplace(boolean createReplace) { this.createReplace = createReplace; }

    /**
     * True for Designer or Gradle runs.
     */
    private boolean standalone;
    public boolean isStandalone() { return standalone; }
    public void setStandalone(boolean standalone) { this.standalone = standalone; }

    public TestExecConfig() {
        // default options
    }

    public TestExecConfig(JSONObject json) throws JSONException {
        if (json.has("threads"))
            this.threads = json.getInt("threads");
        if (json.has("interval"))
            this.interval = json.getInt("interval");
        if (json.has("stubbing"))
            this.stubbing = json.getBoolean("stubbing");
        if (json.has("loadTest"))
            this.loadTest = json.getBoolean("loadTest");
        if (json.has("verbose"))
            this.verbose = json.getBoolean("verbose");
        if (json.has("createReplace"))
            this.createReplace = json.getBoolean("createReplace");
        if (json.has("standalone"))
            this.standalone = json.getBoolean("standalone");
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (threads > 0)
            json.put("threads", threads);
        if (interval > 0)
            json.put("interval", interval);
        if (stubbing)
            json.put("stubbing", stubbing);
        if (loadTest)
            json.put("loadTest", loadTest);
        if (verbose)
            json.put("verbose", verbose);
        if (createReplace)
            json.put("createReplace", createReplace);
        if (standalone)
            json.put("standalone", standalone);

        return json;
    }

    public String getJsonName() {
        return "testExecConfig";
    }
}
