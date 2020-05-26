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

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PackageTests implements Jsonable, Comparable<PackageTests> {

    private final String packageName;
    public String getPackageName() { return packageName; }

    private List<TestCase> testCases;
    public List<TestCase> getTestCases() { return testCases; }
    public void setTestCases(List<TestCase> testCases) { this.testCases = testCases; }

    public PackageTests(String packageName) {
        this.packageName = packageName;
    }

    public PackageTests(JSONObject json) throws JSONException {
        packageName = json.getString("name");
        if (json.has("testCases")) {
            JSONArray tcArr = json.getJSONArray("testCases");
            testCases = new ArrayList<>();
            for (int i = 0; i < tcArr.length(); i++) {
                testCases.add(new TestCase(packageName, tcArr.getJSONObject(i)));
            }
        }
    }

    public JSONObject getJson() throws JSONException {
        JSONObject pkg = create();
        pkg.put("name", packageName);
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

    public int compareTo(PackageTests other) {
        return this.packageName.compareToIgnoreCase(other.packageName);
    }
}