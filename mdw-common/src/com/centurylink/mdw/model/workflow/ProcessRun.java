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

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.util.JsonUtil;

public class ProcessRun implements Jsonable {

    private Long id;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    private String masterRequestId;
    public String getMasterRequestId() { return masterRequestId; }
    public void setMasterRequestId(String s) { masterRequestId = s; }

    private Long definitionId;
    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long id) { this.definitionId = id; }

    private Map<String,Value> values;
    public Map<String,Value> getValues() { return values; }
    public void setValues(Map<String,Value> values) { this.values = values; }

    private String ownerType;
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }

    private Long ownerId;
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    /**
     * Populated after launch.
     */
    private Long instanceId;
    public Long getInstanceId() { return instanceId; }
    public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }

    public ProcessRun() {
    }

    public ProcessRun(JSONObject json) throws JSONException {
        if (json.has("id"))
            this.id = json.getLong("id");
        if (json.has("masterRequestId"))
            this.masterRequestId = json.getString("masterRequestId");
        if (json.has("definitionId"))
            this.definitionId = json.getLong("definitionId");
        if (json.has("values")) {
            this.values = new HashMap<>();
            Map<String,JSONObject> jsonVals = JsonUtil.getJsonObjects(json.getJSONObject("values"));
            for (String name : jsonVals.keySet())
                this.values.put(name, new Value(name, jsonVals.get(name)));
        }
        if (json.has("ownerType"))
            this.ownerType = json.getString("ownerType");
        if (json.has("ownerId"))
            this.ownerId = json.getLong("ownerId");
        if (json.has("instanceId"))
            this.instanceId = json.getLong("instanceId");
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        if (id != null)
            json.put("id", id);
        if (masterRequestId != null)
            json.put("masterRequestId", masterRequestId);
        if (definitionId != null)
            json.put("definitionId", definitionId);
        if (values != null) {
            JSONObject valuesJson = create();
            for (String name : values.keySet()) {
                valuesJson.put(name, values.get(name).getJson());
            }
            json.put("values", valuesJson);
        }
        if (ownerType != null)
            json.put("ownerType", ownerType);
        if (ownerId != null)
            json.put("ownerId", ownerId);
        if (instanceId != null)
            json.put("instanceId", instanceId);
        return json;
    }

    public String getJsonName() {
        return "processRun";
    }
}
