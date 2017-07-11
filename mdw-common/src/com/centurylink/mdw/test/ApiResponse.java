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
package com.centurylink.mdw.test;

import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Status;

public class ApiResponse implements Jsonable {

    public ApiResponse(JSONObject json) {
        bind(json);
    }

    private Status status;
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    private Map<String,String> headers;
    public Map<String,String> getHeaders() { return headers; }
    public void setHeaders(Map<String,String> headers) { this.headers = headers; }

    private int time;
    public int getTime() { return time; }
    public void setTime(int time) { this.time = time; }

    private String body;
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

}
