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
package com.centurylink.mdw.model;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

public class Status implements Jsonable {
    private int code;
    public int getCode() { return code; }

    private String message;
    public String getMessage() { return message; }

    public Status(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public Status(JSONObject json) throws JSONException {
        if (json.has("code"))
            this.code = json.getInt("code");
        if (json.has("message"))
            this.message = json.getString("message");
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (code > 0)
            json.put("code", code);
        if (message != null)
            json.put("message", message);
        return json;
    }

    public String getJsonName() {
        return "status";
    }
}
