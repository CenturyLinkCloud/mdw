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
package com.centurylink.mdw.model.task;

import com.centurylink.mdw.model.Aggregate;
import com.centurylink.mdw.model.Jsonable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Aggregated task instance count for a particular definition, workgroup or user.
 */
public class TaskAggregate implements Aggregate, Jsonable {

    private long value;
    public long getValue() { return value; }
    public void setValue(long value) { this.value = value; }

    private long count = -1;
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    private long id = -1;
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String version = null;
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    private String packageName;
    public String getPackageName() { return packageName; }
    public void setPackageName(String pkg) { this.packageName = pkg; }

    private boolean definitionMissing;
    public void setDefinitionMissing(boolean missing) { this.definitionMissing = missing; }

    private String workgroup;
    public String getWorkgroup() { return workgroup; }
    public void setWorkgroup(String workgroup) { this.workgroup = workgroup; }

    private String userId;
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public TaskAggregate(long value) {
        this.value = value;
    }

    @SuppressWarnings("unused")
    public TaskAggregate(JSONObject json) throws JSONException {
        value = json.getLong("value");
        if (json.has("count"))
            count = json.getLong("count");
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
        if (json.has("userId"))
            userId = json.getString("userId");
        if (json.has("workgroup"))
            workgroup = json.getString("workgroup");
    }

    public String getJsonName() {
        return "taskCount";
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("value", value);
        if (count > -1)
            json.put("count", count);
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
        if (userId != null)
            json.put("id", userId);
        if (workgroup != null)
            json.put("id", workgroup);
        return json;
    }
}
