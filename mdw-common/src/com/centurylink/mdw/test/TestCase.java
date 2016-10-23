/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.test;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.util.StringHelper;

/**
 * Simple test case model for automated testing services.
 * (Designer version is too tightly coupled to test engine to move to mdw-common).
 */
public class TestCase implements Jsonable, Comparable<TestCase> {

    /**
     * Null means not executed.
     */
    public enum Status {
        InProgress,
        Errored,
        Failed,
        Passed
    }

    public TestCase(String pkg, AssetInfo asset) {
        this.pkg = pkg;
        this.asset = asset;
    }

    private AssetInfo asset;
    public AssetInfo getAsset() { return asset; }

    public String getName() {
        return asset.getName();
    }

    private String pkg;
    public String getPackage() { return pkg; }

    public String getPath() {
        return pkg + "/" + asset.getName();
    }

    private Status status;
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    private Date start;
    public Date getStart() { return start; }
    public void setStart(Date start) { this.start = start; }

    private Date end;
    public Date getEnd() { return end; }
    public void setEnd(Date end) { this.end = end; }

    private String message;
    public String getMessage() { return message; }
    public void setMessage(String msg) { this.message = msg; }

    // package/asset path for expected results
    private String expected;
    public String getExpected() { return expected; }
    public void setExpected(String expected) { this.expected = expected; }

    // file path relative to expected results dir
    private String actual;
    public String getActual() { return actual; }
    public void setActual(String actual) { this.actual = actual; }

    // file path relative to expected results dir
    private String executeLog;
    public String getExecuteLog() { return executeLog; }
    public void setExecuteLog(String executeLog) { this.executeLog = executeLog; }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", getName());
        if (start != null)
            json.put("start", StringHelper.serviceDateToString(start));
        if (end != null)
            json.put("end", StringHelper.serviceDateToString(end));
        if (status != null)
            json.put("status", status.toString());
        if (message != null)
            json.put("message", message);
        if (expected != null)
            json.put("expected", expected);
        if (actual != null)
            json.put("actual", actual);
        if (executeLog != null)
            json.put("executeLog", executeLog);

        return json;
    }

    public String getJsonName() {
        return "TestCase";
    }

    public int compareTo(TestCase other) {
        return this.getAsset().compareTo(other.getAsset());
    }

}
