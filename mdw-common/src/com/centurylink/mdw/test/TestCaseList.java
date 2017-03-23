/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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

    private List<PackageTests> packageTests;
    public List<PackageTests> getPackageTests() { return packageTests; }
    public void setPackageTests(List<PackageTests> packageTests) { this.packageTests = packageTests; }

    public PackageTests getPackageTests(String packageName) {
        if (packageTests != null) {
            for (PackageTests pkgTests : packageTests) {
                if (pkgTests.getPackageDir().getPackageName().equals(packageName))
                    return pkgTests;
            }
        }
        return null;
    }

    public void addPackageTests(PackageTests pkgTests) {
        if (packageTests == null) {
            packageTests = new ArrayList<PackageTests>();
        }
        packageTests.add(pkgTests);
    }

    public List<TestCase> getTestCases() {
        List<TestCase> testCases = new ArrayList<TestCase>();
        for (PackageTests pkgTestCases : getPackageTests()) {
            for (TestCase testCase : pkgTestCases.getTestCases()) {
                testCases.add(testCase);
            }
        }
        return testCases;
    }

    public TestCase getTestCase(String path) {
        for (PackageTests pkgTests : packageTests) {
            if (path.startsWith(pkgTests.getPackageDir().getPackageName() + "/")) {
                for (TestCase pkgTest : pkgTests.getTestCases()) {
                    if (pkgTest.getPath().equals(path))
                        return pkgTest;
                }
            }
        }
        return null;
    }

    public TestCase addTestCase(TestCase testCase) {
        for (PackageTests pkgTests : packageTests) {
            if (testCase.getPath().startsWith(pkgTests.getPackageDir().getPackageName() + "/")) {
                pkgTests.getTestCases().add(testCase);
                return testCase;
            }
        }
        return null;
    }

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
            this.packageTests = new ArrayList<PackageTests>();
            for (int i = 0; i < pkgsArr.length(); i++) {
                this.packageTests.add(new PackageTests(assetRoot, pkgsArr.getJSONObject(i)));
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
        if (packageTests != null) {
            for (PackageTests pkgTests : packageTests)
                array.put(pkgTests.getJson());
        }
        json.put("packages", array);
        return json;
    }

    public String getJsonName() {
        return "AutomatedTests";
    }

    public void sort() {
        Collections.sort(getPackageTests());
        for (PackageTests pkgTests : getPackageTests()) {
            if (pkgTests.getTestCases() != null)
                Collections.sort(pkgTests.getTestCases());
        }
    }

}
