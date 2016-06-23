/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.event;

import java.io.Serializable;
import java.text.ParseException;

import org.json.JSONException;
import org.json.JSONObject;

public class ExternalMessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String request;
    private String response;

    public ExternalMessageVO(String request, String response) {
        this.request = request;
        this.response = response;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public ExternalMessageVO(String json) throws JSONException, ParseException {
        this(new JSONObject(json));
    }

    public ExternalMessageVO(JSONObject jsonObj) throws JSONException, ParseException {
        if (jsonObj.has("request"))
            request = jsonObj.getString("request");
        if (jsonObj.has("response"))
            response = jsonObj.getString("response");
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (request != null)
            json.put("request", request);
        if (response != null)
            json.put("response", response);
        return json;
    }

}
