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
package com.centurylink.mdw.model.request;

import com.centurylink.mdw.model.Aggregate;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.StatusResponse;
import org.json.JSONException;
import org.json.JSONObject;

public class RequestAggregate implements Aggregate, Jsonable {

    private long id;
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    private String path;
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getName() {
        return getPath() == null ? StatusResponse.getMessage(status) : getPath();
    }

    private Integer status;
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    private long value;
    public long getValue() { return value; }
    public void setValue(long value) { this.value = value; }

    private long count = -1;
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    public RequestAggregate(long value) {
        this.value = value;
    }

    @SuppressWarnings("unused")
    public RequestAggregate(JSONObject json) throws JSONException {
        value = json.getLong("value");
        if (json.has("count"))
            count = json.getLong("count");
        if (json.has("path"))
            path = json.getString("path");
        else if (json.has("name"))
            path = json.getString("name");
        if (json.has("status"))
            status = json.getInt("status");
    }

    public String getJsonName() {
        return "requestCount";
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("value", value);
        if (count > -1)
            json.put("count", count);
        if (path != null) {
            json.put("id", path);
            json.put("name", path);
        }
        else if (id > 0) {
            json.put("id", id);
        }
        if (status != null) {
            json.put("status", status);
            json.put("name", StatusResponse.getMessage(status));
        }
        return json;
    }
}
