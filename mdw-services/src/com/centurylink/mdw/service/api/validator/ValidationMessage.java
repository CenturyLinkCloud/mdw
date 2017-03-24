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
