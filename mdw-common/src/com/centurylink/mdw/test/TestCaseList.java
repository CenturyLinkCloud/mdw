/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.util.StringHelper;

public class TestCaseList implements Jsonable {

    private String suite;
    public String getSuite() { return suite; }
    public void setSuite(String suite) { this.suite = suite; }

    private Date retrieveDate;
    public Date getRetrieveDate() { return retrieveDate; }
    public void setRetrieveDate(Date d) { this.retrieveDate = d; }

    private int count;
    public int getCount() { return count; }
    public void setCount(int ct) { this.count = ct; }

    private List<PackageTests> testCases;
    public List<PackageTests> getTestCases() { return testCases; }
    public void setTestCases(List<PackageTests> testCases) { this.testCases = testCases; }

    private File assetRoot;

    public TestCaseList(File assetRoot) {
        this.assetRoot = assetRoot;
    }

    public TestCaseList(File assetRoot, JSONObject json) throws JSONException {
        if (json.has("suite"))
            this.suite = json.getString("suite");
        if (json.has("assetRoot"))
            this.assetRoot = new File(json.getString("assetRoot"));
        if (json.has("retrieveDate"))
            this.retrieveDate = StringHelper.serviceStringToDate(json.getString("retrieveDate"));
        if (json.has("count"))
            this.count = json.getInt("count");
        if (json.has("packages")) {
            JSONArray pkgsArr = json.getJSONArray("packages");
            this.testCases = new ArrayList<PackageTests>();
            for (int i = 0; i < pkgsArr.length(); i++) {
                this.testCases.add(new PackageTests(assetRoot, pkgsArr.getJSONObject(i)));
            }
        }
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (suite != null)
            json.put("suite", suite);
        if (assetRoot != null)
            json.put("assetRoot", assetRoot);
        json.put("retrieveDate", StringHelper.serviceDateToString(getRetrieveDate()));
        json.put("count", count);
        JSONArray array = new JSONArray();
        if (testCases != null) {
            for (PackageTests packageTests : testCases)
                array.put(packageTests.getJson());
        }
        json.put("packages", array);
        return json;
    }

    public String getJsonName() {
        return "AutomatedTests";
    }

}
