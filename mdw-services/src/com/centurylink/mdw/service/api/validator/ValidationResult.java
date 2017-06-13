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
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Jsonable;

/**
 * @deprecated Validations throw ServiceException
 */
@Deprecated
public class ValidationResult implements Jsonable {
    private List<ValidationMessage> validationMessages;

    public ValidationResult(JSONObject json) throws JSONException, ParseException {
        if (json.has("validationMessages")) {
            this.validationMessages = new ArrayList<ValidationMessage>();
            JSONArray jsonArr = json.getJSONArray("validationMessages");
            for (int i = 0; i < jsonArr.length(); i++)
                this.validationMessages.add(new ValidationMessage(jsonArr.getJSONObject(i)));
        }
    }
    public ValidationResult() {
        // TODO Auto-generated constructor stub
    }
    public List<ValidationMessage> getValidationMessages() {
        return validationMessages;
    }

    public void setValidationMessages(List<ValidationMessage> validationMessages) {
        this.validationMessages = validationMessages;
    }

    public String getConcatenatedMessages() {
        StringBuffer concatenated = new StringBuffer();
        if (validationMessages != null) {
            validationMessages.forEach((message)-> concatenated.append(message.getMessage()).append(";"));
        }
        return concatenated.toString();
    }
    public ValidationResult addValidationMessage(ValidationMessage validationMessage) {
        if (validationMessage != null) {
            if (validationMessages == null)
                validationMessages = new ArrayList<ValidationMessage>();
            validationMessages.add(validationMessage);
        }
        return this;
    }

    public ValidationResult addValidationMessages(ValidationResult result) {
        if (result != null && result.getValidationMessages() != null) {
            result.getValidationMessages().forEach((message) -> addValidationMessage(message));
        }
        return this;
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JsonObject();
        if (validationMessages != null) {
            JSONArray jsonArr = new JSONArray();
            for (ValidationMessage message : validationMessages)
                jsonArr.put(message.getJson());
            json.put("validationMessages", jsonArr);
        }
        return json;
    }



    @Override
    public String getJsonName() {
        // TODO Auto-generated method stub
        return "validationResult";
    }
}
