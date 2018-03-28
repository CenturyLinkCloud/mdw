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
package com.centurylink.mdw.common.service.types;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="StatusMessage", description="MDW service response (embedded as 'status' object)")
public class StatusMessage implements Jsonable {

    private Integer code;
    private String message;
    private String requestId;

    public StatusMessage() {
    }

    public StatusMessage(int code, String message) {
        setCode(code);
        setMessage(message);
    }

    public StatusMessage(JSONObject jsonObj) throws JSONException {
        JSONObject status = jsonObj.getJSONObject("status");
        if (status.has("code"))
            this.setCode(status.getInt("code"));
        if (status.has("message"))
            this.setMessage(status.getString("message"));
        if (status.has("requestId"))
            this.setRequestId(status.getString("requestId"));
    }

    public StatusMessage(Throwable t) {
        setCode(500);
        setMessage(t.toString());
    }

    public StatusMessage(Throwable t, String serverName) {
        setCode(500);
        setMessage(serverName);
    }

    public String getMessage() {
        return message;
    }

    public String getRequestId() {
        return requestId;
    }

    public Integer getCode() {
        return code;
    }

    public boolean isSuccess() {
        return getCode() != null && getCode() == 0;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @ApiModelProperty(hidden=true)
    public JSONObject getJson() throws JSONException {
        JSONObject status = create();
        Integer code = getCode();
        if (code != null)
            status.put("code", code.intValue());
        String message = getMessage();
        if (message != null)
            status.put("message", message);
        String requestId = getRequestId();
        if (requestId != null)
            status.put("requestId", requestId);
        JSONObject json = create();
        json.put("status", status);
        return json;
    }

    @ApiModelProperty(hidden=true)
    public String getJsonString() {
        try {
            return getJson().toString(2);
        }
        catch (JSONException ex) {
            return "{ \"status\": {  \"code\": " + getCode() + ", \"message\": \"" + getMessage() + "\" } }";
        }
    }

    @ApiModelProperty(hidden=true)
    public String getJsonName() { return "status"; }

    public String getXml() {
        StringBuilder xml = new StringBuilder();
        xml.append("<bpm:MDWStatusMessage xmlns:bpm=\"http://mdw.centurylink.com/bpm\">\n");
        if (code != null)
            xml.append("<StatusCode>").append(code).append("</StatusCode>\n");
        if (message != null)
            xml.append("<bpm:StatusMessage>").append(message).append("</bpm:StatusMessage>\n");
        xml.append("</bpm:MDWStatusMessage>");
        return xml.toString();
    }
}
