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

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.model.Category;

public class TaskCategory extends Category implements Jsonable, Comparable<TaskCategory> {

    public TaskCategory(Long id, String code, String name) {
        setId(id);
        setCode(code);
        setName(name);
    }

    public TaskCategory(JSONObject json) throws JSONException {
        this.setId(json.getLong("id"));
        this.setCode(json.getString("code"));
        this.setName(json.getString("name"));
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", getId());
        json.put("code", getCode());
        json.put("name", getName());
        return json;
    }

    public String getName() {
        return getDescription();
    }
    public void setName(String name) {
        setDescription(name);
    }

    public int compareTo(TaskCategory other) {
        if (this.getName() == null)
            this.setName(""); // bad data
        return this.getName().compareTo(other.getName());
    }

    public String getJsonName() {
        return "TaskCategory";
    }
}
