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

public class Response implements Jsonable {

    private String content;
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    /**
     * Response content as object (for non-Poolable Adapter).
     */
    private Object object;
    public Object getObject() { return object; }
    public void setObject(Object object) { this.object = object; }

    private Integer statusCode;
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer code) { this.statusCode = code; }

    private String statusMessage;
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String message) { this.statusMessage = message; }

    private JSONObject meta;
    public JSONObject getMeta() { return meta; }
    public void setMeta(JSONObject info) { meta = info; }

    public Response() {
    }

    public Response(String content) {
        this.content = content;
    }

    public Response(JSONObject json) throws JSONException {
        if (json.has("content"))
            this.content = json.getString("content");
        if (json.has("statusCode"))
            this.statusCode = json.getInt("statusCode");
        if (json.has("statusMessage"))
            this.statusMessage = json.getString("statusMessage");
    }

    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (content != null)
            json.put("content", content);
        if (statusCode != null)
            json.put("statusCode", statusCode);
        if (statusMessage != null)
            json.put("statusMessage", statusMessage);
        return json;
    }

    public String getJsonName() {
        return "response";
    }



}
