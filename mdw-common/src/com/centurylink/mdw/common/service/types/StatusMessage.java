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

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument.MDWStatusMessage;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.xml.XmlBeanWrapper;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="StatusMessage", description="MDW service response (embedded as 'status' object)")
public class StatusMessage extends XmlBeanWrapper implements Jsonable {

    public StatusMessage() {
        super(MDWStatusMessageDocument.Factory.newInstance());
    }

    public StatusMessage(MDWStatusMessageDocument statusDoc) {
        super(statusDoc);
    }

    public StatusMessage(String statusXml) throws XmlException {
        super();
        fromXml(statusXml);
    }

    public StatusMessage(int code, String message) {
        super(MDWStatusMessageDocument.Factory.newInstance());
        setCode(code);
        setMessage(message);
    }

    public StatusMessage(JSONObject jsonObj) throws JSONException, XmlException {
        this();
        JSONObject status = jsonObj.getJSONObject("status");
        if (status.has("code"))
            this.setCode(status.getInt("code"));
        if (status.has("message"))
            this.setMessage(status.getString("message"));
        if (status.has("requestId"))
            this.setRequestId(status.getString("requestId"));
    }

    public StatusMessage(Throwable t) throws XmlException {
        this();
        setCode(-1);
        setMessage(t.toString());
    }

    public StatusMessage(Throwable t, String serverName) throws XmlException {
        this();
        setCode(-1);
        setMessage(serverName);
    }

    public void fromXml(String xml) throws XmlException {
        XmlOptions xmlOptions = super.getXmlLoadOptions();
        xmlOptions.setDocumentType(MDWStatusMessageDocument.type);
        setXmlBean(MDWStatusMessageDocument.Factory.parse(xml, xmlOptions));
    }

    @ApiModelProperty(hidden=true)
    public MDWStatusMessageDocument getStatusDocument() {
        return (MDWStatusMessageDocument) getXmlBean();
    }

    @ApiModelProperty(hidden=true)
    public MDWStatusMessage getStatus() {
        return getStatusDocument().getMDWStatusMessage();
    }

    public String getMessage() {
        if (getStatus() == null)
            return null;
        return getStatus().getStatusMessage();
    }

    public String getRequestId() {
        if (getStatus() == null)
            return null;
        return getStatus().getRequestID();
    }

    public Integer getCode() {
        if (getStatus() == null)
            return null;
        return getStatus().getStatusCode();
    }

    public boolean isSuccess() {
        return getCode() != null && getCode() == 0;
    }

    public void setStatus(MDWStatusMessage status) {
        getStatusDocument().setMDWStatusMessage(status);
    }

    public void setMessage(String message) {
        if (getStatus() == null)
            getStatusDocument().addNewMDWStatusMessage();
        getStatus().setStatusMessage(message);
    }

    public void setCode(int code) {
        if (getStatus() == null)
            getStatusDocument().addNewMDWStatusMessage();
        getStatus().setStatusCode(code);
    }

    public void setRequestId(String requestId) {
        if (getStatus() == null)
            getStatusDocument().addNewMDWStatusMessage();
        getStatus().setRequestID(requestId);
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

    public String toXml() {
        return getXml();
    }
}
