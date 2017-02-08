/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */

package com.centurylink.mdw.service.api.validator;

import java.text.ParseException;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

/**
 * Dynamic Java workflow asset.
 */
public class ValidationMessage implements Jsonable {
    private ValidationError code;
    private String message;

    public ValidationMessage(JSONObject json) throws JSONException, ParseException {
        if (json.has("message"))
            this.message = json.getString("message");
    }
    public ValidationMessage() {
        // TODO Auto-generated constructor stub
    }
    public ValidationMessage code(ValidationError code) {
        this.code = code;
        return this;
    }
    public ValidationMessage message(String message) {
        this.message = message;
        return this;
    }

    public ValidationError getCode() {
        return code;
    }
    public void setCode(ValidationError code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    @Override
    public JSONObject getJson() throws JSONException {
        // TODO Auto-generated method stub
        JSONObject json = new JSONObject();
        if (message !=null) {
            json.put("message", message);
        }
        return json;
    }
    @Override
    public String getJsonName() {
        // TODO Auto-generated method stub
        return "validationMessage";
    }
}
