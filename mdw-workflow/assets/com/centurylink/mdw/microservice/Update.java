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

import io.swagger.annotations.ApiModel;

/**
 * An update from a microservice.
 */
@ApiModel(description = "An update from a microservice")
public class Update implements Jsonable {

    private Long requestId;
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    private Status status;
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    private Date received;
    public Date getReceived() { return received; }
    public void setReceived(Date received) { this.received = received; }

    public Update(Long requestId, Status status, Date received) {
        this.requestId = requestId;
        this.status = status;
        this.received = received;
    }
    public Update(JSONObject json) throws JSONException {
        if (json.has("requestId"))
            this.requestId = json.getLong("requestId");
        if (json.has("status"))
            this.status = new Status(json.getJSONObject("status"));
        if (json.has("received"))
            this.received = StringHelper.stringToDate(json.getString("received"));
    }

    /**
     * Microservice is name, so not included.
     */
    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        if (requestId != null)
            json.put("requestId", requestId);
        if (received != null)
            json.put("received", StringHelper.dateToString(received));
        if (status != null)
            json.put("status", status.getJson());

        return json;
    }

    public String getJsonName() {
        return "update";
    }
}
