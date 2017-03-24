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

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.Property;

/**
 * Dynamic Java workflow asset.
 */
public class BooleanValidator implements Validator {
    @Override
    public ValidationResult validate(JSONObject json, String key, Property modelProperty,
            Iterator<Validator> next) {
        ValidationResult result = new ValidationResult();
        if (modelProperty instanceof BooleanProperty) {
            try {
                // JSONObject does a pretty good job of parsing boolean
                if ((json.has(key) || modelProperty.getRequired()) && !json.getBoolean(key)) {
                    result = new ValidationResult()
                            .addValidationMessage(new ValidationMessage().message("property " + key
                                    + " is not a valid boolean value (true or false)"));
                }
            }
            catch (JSONException e) {
                result = new ValidationResult().addValidationMessage(
                        new ValidationMessage().message("Unable to parse property " + key
                                + " from json " + json.toString() + ":" + e.getMessage()));
            }
        }
        if(next.hasNext()) {
            result.addValidationMessages(next.next().validate(json, key, modelProperty, next));
        }
        return result;
    }

}
