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

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.InstanceCount;
import com.centurylink.mdw.model.Jsonable;

public class RequestCount implements InstanceCount, Jsonable {

    private long count = -1;
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    private String type;
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public RequestCount(long count) {
        this.count = count;
    }

    public RequestCount(JSONObject json) throws JSONException {
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
