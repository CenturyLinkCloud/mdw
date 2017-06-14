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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
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
        Waiting,
        InProgress,
        Errored,
        Failed,
        Passed,
        Stopped // terminated by user
    }

    public TestCase(String pkg, AssetInfo asset) {
        this.pkg = pkg;
        this.asset = asset;
    }

    private AssetInfo asset;
    public AssetInfo getAsset() { return asset; }

    /**
     * impl-specific subtests (eg: postman collection)
     */
    private List<TestCaseItem> items;
    public List<TestCaseItem> getItems() { return items; }
    public void addItem(TestCaseItem item) {
        if (items == null)
            items = new ArrayList<>();
        items.add(item);
    }
    public TestCaseItem getItem(String name) {
        if (items != null) {
            for (TestCaseItem item : items) {
                if (item.getJson().get("name").equals(name))
                    return item;
            }
        }
        return null;
    }

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

    public boolean isFinished() {
        return getStatus() != null && getStatus() != Status.Waiting
                && getStatus() != Status.InProgress;
    }

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

    public TestCase(File assetRoot, String pkg, JSONObject json) throws JSONException {
        this.asset = new AssetInfo(assetRoot, pkg + "/" + json.getString("name"));
        this.pkg = pkg;
        if (json.has("start"))
            this.start = StringHelper.serviceStringToDate(json.getString("start"));
        if (json.has("end"))
            this.end = StringHelper.serviceStringToDate(json.getString("end"));
        if (json.has("status"))
            this.status = Status.valueOf(json.getString("status"));
        if (json.has("message"))
            this.message = json.getString("message");
        if (json.has("expected"))
            this.expected = json.getString("expected");
        if (json.has("actual"))
            this.actual = json.getString("actual");
        if (json.has("executeLog"))
            this.executeLog = json.getString("executeLog");
        if (json.has("items")) {
            this.items = new ArrayList<>();
            JSONArray arr = json.getJSONArray("items");
            for (int i = 0; i < arr.length(); i++) {
                this.items.add(new TestCaseItem(arr.getJSONObject(i)));
            }
        }
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
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
        if (items != null)
            json.put("items", items);
        if (asset.getCommitInfo() != null)
            json.put("commitInfo", asset.getCommitInfo().getJson());
        if (items != null) {
            JSONArray arr = new JSONArray();
            for (TestCaseItem item : items)
                arr.put(item.getJson());
            json.put("items", arr);
        }

        return json;
    }

    public String getJsonName() {
        return "TestCase";
    }

    public int compareTo(TestCase other) {
        return this.getAsset().compareTo(other.getAsset());
    }

    public File file() {
        return asset.getFile();
    }

    public String getText() throws IOException {
        return text();
    }

    public String text() throws IOException {
        return new String(read(file()));
    }

    private byte[] read(File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            return bytes;
        }
        finally {
            if (fis != null)
                fis.close();
        }
    }
}
