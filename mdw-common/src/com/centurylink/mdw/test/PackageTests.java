/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.test;

import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.dataaccess.file.PackageDir;

public class PackageTests implements Jsonable {

    private PackageDir packageDir;
    public PackageDir getPackageDir() { return packageDir; }

    private List<TestCase> testCases;
    public List<TestCase> getTestCases() { return testCases; }
    public void setTestCases(List<TestCase> testCases) { this.testCases = testCases; }

    public PackageTests(PackageDir pkgDir) {
        this.packageDir = pkgDir;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject pkg = new JSONObject();
        pkg.put("name", packageDir.getPackageName());
        pkg.put("version", packageDir.getPackageVersion());
        JSONArray testCaseArray = new JSONArray();
        if (testCases != null) {
            for (TestCase testCase : testCases)
                testCaseArray.put(testCase.getJson());
        }
        pkg.put("testCases", testCaseArray);
        return pkg;
    }

    public String getJsonName() {
        return "PackageTests";
    }

    public void sort() {
        Collections.sort(testCases);
    }
}