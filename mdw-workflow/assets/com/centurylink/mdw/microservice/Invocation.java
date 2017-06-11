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
package com.centurylink.mdw.microservice;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.util.StringHelper;

/**
 * A single invocation of a microservice.
 */
public class Invocation implements Jsonable {

    private Long requestId;
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    private Long responseId;
    public Long getResponseId() { return responseId; }
    public void setResponseId(Long responseId) { this.responseId = responseId; }

    private Status status;
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    private Date sent;
    public Date getSent() { return sent; }
    public void setSent(Date sent) { this.sent = sent; }

    public Invocation(Long requestId, Status status, Date sent) {
        this.requestId = requestId;
        this.status = status;
        this.sent = sent;
    }
    public Invocation(Long requestId, Status status, Date sent, Long responseId) {
        this.requestId = requestId;
        this.responseId = responseId;
        this.status = status;
        this.sent = sent;
    }
    public Invocation(JSONObject json) throws JSONException {
        if (json.has("requestId"))
            this.requestId = json.getLong("requestId");
        if (json.has("status"))
            this.status = new Status(json.getJSONObject("status"));
        if (json.has("sent"))
            this.sent = StringHelper.stringToDate(json.getString("sent"));
        if (json.has("responseId"))
            this.responseId = json.getLong("responseId");
    }

    /**
     * Microservice is name, so not included.
     */
    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        if (requestId != null)
            json.put("requestId", requestId);
        if (status != null)
            json.put("status", status.getJson());
        if (sent != null)
            json.put("sent", StringHelper.dateToString(sent));
        if (responseId != null) {
            json.put("responseId", responseId);
        }
        return json;
    }

    public String getJsonName() {
        return "invocation";
    }
}
