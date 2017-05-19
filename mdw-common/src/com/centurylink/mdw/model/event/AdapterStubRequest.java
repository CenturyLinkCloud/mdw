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

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.util.JsonUtil;

public class AdapterStubRequest implements Jsonable {

    public static final String JSON_NAME = "AdapterStubRequest";

    private String masterRequestId;
    public String getMasterRequestId() { return masterRequestId; }
    public void setMasterRequestId(String masterRequestId) { this.masterRequestId = masterRequestId; }

    private String url;
    public String getUrl() { return url; }
    public void setUrl(String endpoint) { this.url = endpoint; }

    private String method;
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    private Map<String,String> headers;
    public Map<String,String> getHeaders() { return headers; }
    public void setHeaders(Map<String,String> headers) { this.headers = headers; }

    private String content;
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public AdapterStubRequest(String masterRequestId, String content) {
        this.masterRequestId = masterRequestId;
        this.content = content;
    }

    public AdapterStubRequest(JSONObject json) throws JSONException {
        JSONObject requestJson = json.getJSONObject(getJsonName());
        this.masterRequestId = requestJson.getString("masterRequestId");
        this.content = requestJson.getString("content");
        if (requestJson.has("url"))
            this.url = requestJson.getString("url");
        if (requestJson.has("method"))
            this.method = requestJson.getString("method");
        if (requestJson.has("headers"))
            this.headers = JsonUtil.getMap(requestJson.getJSONObject("headers"));
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        JSONObject requestJson = create();
        requestJson.put("masterRequestId", masterRequestId);
        requestJson.put("content", content);
        if (url != null)
            requestJson.put("url", url);
        if (method != null)
            requestJson.put("method", method);
        if (headers != null)
            requestJson.put("headers", JsonUtil.getJson(headers));
        json.put(getJsonName(), requestJson);
        return json;
    }

    public String getJsonName() {
        return JSON_NAME;
    }

}
