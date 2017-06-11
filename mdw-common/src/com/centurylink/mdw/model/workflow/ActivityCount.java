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

import com.centurylink.mdw.util.StringHelper;

/**
 * Aggregated activity instance count for a particular definition or status.
 */
public class ActivityCount extends ProcessCount {

    private long processId;
    public long getProcessId() { return processId; }
    public void setProcessId(long processId) { this.processId = processId; }

    private String activityId;
    public String getActivityId() { return activityId; }
    public void setActivityId(String id) { this.activityId = id; }

    private String definitionId;
    public String getDefinitionId() { return definitionId;}
    public void setDefinitionId(String definitionId) { this.definitionId = definitionId; }

    // separate from name, which includes process qualifier
    private String activityName;
    public String getActivityName() { return activityName; }
    public void setActivityName(String name) { this.activityName = name; }

    private String processName;
    public String getProcessName() { return processName; }
    public void setProcessName(String name) { this.processName = name; }

    public ActivityCount(long count) {
        super(count);
    }

    public ActivityCount(JSONObject json) throws JSONException {
        super(json.getLong("count"));
        if (json.has("id"))
            activityId = json.getString("id");
        if (json.has("name"))
            setName(json.getString("name"));
        if (json.has("processId"))
            processId = json.getLong("processId");
        if (json.has("definitionId"))
            definitionId = json.getString("definitionId");
        if (json.has("activityName"))
            activityName = json.getString("activityName");
        if (json.has("processName"))
            processName = json.getString("processName");
        if (json.has("version"))
            setVersion(json.getString("version"));
        if (json.has("packageName"))
            setPackageName(json.getString("packageName"));
        if (json.has("definitionMissing"))
            setDefinitionMissing(json.getBoolean("definitionMissing"));
        if (json.has("status"))
            setStatus(json.getString("status"));
    }

    public String getJsonName() {
        return "activityCount";
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("count", getCount());
        if (!StringHelper.isEmpty(activityId))
            json.put("id", activityId);
        if (getName() != null)
            json.put("name", getName());
        if (processId > 0)
            json.put("processId", processId);
        if (definitionId != null)
            json.put("definitionId", definitionId);
        if (activityName != null)
            json.put("activityName", activityName);
        if (processName != null)
            json.put("processName", processName);
        if (getVersion() != null)
            json.put("version", getVersion());
        if (getPackageName() != null)
            json.put("packageName", getPackageName());
        if (isDefinitionMissing())
            json.put("definitionMissing", true);
        if (getStatus() != null)
            json.put("status", getStatus());
        return json;
    }
}
