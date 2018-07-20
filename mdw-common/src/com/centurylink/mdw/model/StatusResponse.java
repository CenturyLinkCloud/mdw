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

import com.centurylink.mdw.model.Jsonable;

/**
 * Standard status response.
 * Lighter-weight, compatible alternative to StatusMessage object.
 */
public class StatusResponse implements Jsonable {

    private Status status;
    public Status getStatus() { return status; }

    public StatusResponse(Status status) {
        this.status = status;
    }

    public StatusResponse(int code, String message) {
        this.status = new Status(code, message);
    }

    public StatusResponse(JSONObject json) throws JSONException {
        this.status = new Status(json.getJSONObject("status"));
    }

    public StatusResponse(Status status, String message) {
        this.status = new Status(status, message);
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("status", status.getJson());
        return json;
    }

    public String getJsonName() {
        return "statusResponse";
    }
}
