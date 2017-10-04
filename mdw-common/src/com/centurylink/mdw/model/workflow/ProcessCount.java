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
package com.centurylink.mdw.model.workflow;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.InstanceCount;
import com.centurylink.mdw.model.Jsonable;

/**
 * Aggregated process instance count for a particular definition or status.
 */
public class ProcessCount implements InstanceCount, Jsonable {

    private long id;
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String version = "0";
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    private String packageName;
    public String getPackageName() { return packageName; }
    public void setPackageName(String pkg) { this.packageName = pkg; }

    private String status;
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    private long count = -1;
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    private long meanCompletionTime = -1;
    public long getMeanCompletionTime() { return meanCompletionTime; }
    public void setMeanCompletionTime(long meanCompletionTime) { this.meanCompletionTime = meanCompletionTime; }

    private boolean definitionMissing;
    public boolean isDefinitionMissing() { return definitionMissing; }
    public void setDefinitionMissing(boolean missing) { this.definitionMissing = missing; }

    public ProcessCount(long count) {
        this.count = count;
    }

    public ProcessCount(JSONObject json) throws JSONException {
        count = json.getLong("count");
        meanCompletionTime = json.getLong("meanCompletionTime");
        if (json.has("id"))
            id = json.getLong("id");
        if (json.has("name"))
            name = json.getString("name");
        if (json.has("version"))
            version = json.getString("version");
        if (json.has("packageName"))
            packageName = json.getString("packageName");
        if (json.has("definitionMissing"))
            definitionMissing = json.getBoolean("definitionMissing");
        if (json.has("status"))
            status = json.getString("status");
    }

    public String getJsonName() {
        return "processCount";
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("count", count);
        json.put("meanCompletionTime", meanCompletionTime);
        if (id >= 0)
            json.put("id", id);
        if (name != null)
            json.put("name", name);
        if (version != null)
            json.put("version", version);
        if (packageName != null)
            json.put("packageName", packageName);
        if (definitionMissing)
            json.put("definitionMissing", true);
        if (status != null)
            json.put("status", status);
        return json;
    }
}
