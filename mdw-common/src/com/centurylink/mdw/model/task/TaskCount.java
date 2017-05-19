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

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.InstanceCount;
import com.centurylink.mdw.model.Jsonable;

/**
 * Aggregated task instance count for a particular definition, workgroup or user.
 */
public class TaskCount implements InstanceCount, Jsonable {

    private long count;
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

    private String workgroup;
    public String getWorkgroup() { return workgroup; }
    public void setWorkgroup(String workgroup) { this.workgroup = workgroup; }

    private String userId;
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    private String userName;
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    private String status;
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    private boolean definitionMissing;
    public boolean isDefinitionMissing() { return definitionMissing; }
    public void setDefinitionMissing(boolean missing) { this.definitionMissing = missing; }

    public TaskCount(long count) {
        this.count = count;
    }

    public TaskCount(JSONObject json) throws JSONException {
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
        if (json.has("workgroup"))
            workgroup = json.getString("workgroup");
        if (json.has("userId"))
            userId = json.getString("userId");
        if (json.has("userName"))
            userName = json.getString("userName");
        if (json.has("status"))
            status = json.getString("status");
    }

    public String getJsonName() {
        return "taskCount";
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
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
        if (workgroup != null)
            json.put("workgroup", workgroup);
        if (userId != null)
            json.put("user", userId);
        if (userName != null)
            json.put("userName", userName);
        if (status != null)
            json.put("status", status);
        return json;
    }
}
