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
package com.centurylink.mdw.model.event;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

/**
 * Represents a workflow event notification.
 */
public class Event implements Jsonable {

    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    private String message;
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    private int delay;
    public int getDelay() { return delay; }
    public void setDelay(int delay) { this.delay = delay; }

    public Event() {
    }

    public Event(JSONObject json) throws JSONException {
        this.id = json.getString("id");
        if (json.has("message"))
            this.message = json.getString("message");
        if (json.has("delay"))
            this.delay = json.getInt("delay");
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        if (message != null)
            json.put("message", message);
        if (delay > 0)
            json.put("delay", delay);
        return json;
    }

    @Override
    public String getJsonName() {
        return "event";
    }

}
