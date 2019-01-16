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
        return getPath();
    }

    private long value;
    public long getValue() { return value; }
    public void setValue(long value) { this.value = value; }

    private long count = -1;
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    private String type;
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public RequestAggregate(long count) {
        this.count = count;
    }

    @SuppressWarnings("unused")
    public RequestAggregate(JSONObject json) throws JSONException {
        count = json.getLong("count");
        if (json.has("type"))
            type = json.getString("type");
    }

    public String getJsonName() {
        return "requestCount";
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("count", count);
        if (type != null)
            json.put("type", type);
        return json;
    }
}
