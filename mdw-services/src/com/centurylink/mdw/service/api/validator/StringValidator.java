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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;

/**
 * @deprecated
 */
@Deprecated
public class StringValidator implements Validator {
    @Override
    public ValidationResult validate(JSONObject json, String key, Property modelProperty,
            Iterator<Validator> next) {
        ValidationResult result = new ValidationResult();
        if (modelProperty instanceof StringProperty) {
            try {
                StringProperty stringProperty = (StringProperty) modelProperty;
                // Check for allowable values
                if (stringProperty.getEnum() != null && stringProperty.getEnum().size() > 0) {
                    List<?> values = stringProperty.getEnum();
                    Set<String> allowable = new LinkedHashSet<String>();
                    for (Object obj : values) {
                        allowable.add(obj.toString());
                    }
                    if (!allowable.contains(json.getString(key))) {
                        result = new ValidationResult().addValidationMessage(new ValidationMessage()
                                .message(("`" + key + "` value `" + json.getString(key)
                                        + "` is not in the allowable values `" + allowable + "`")));
                    }
                }
                if ((stringProperty.getMinLength() !=null && stringProperty.getMinLength() > 0) || (stringProperty.getMaxLength()!=null && stringProperty.getMaxLength() > 0 )) {
                    result.addValidationMessage(validateMinMaxLength(key, json.getString(key), stringProperty.getMinLength(), stringProperty.getMaxLength()));
                }
                // TBD Support date and date-time
            }
            catch (JSONException e) {
                result = new ValidationResult().addValidationMessage(
                        new ValidationMessage().message("Unable to parse property " + key
                                + " from json " + json.toString() + ":" + e.getMessage()));
            }
        }
        if (next.hasNext()) {
            result.addValidationMessages(next.next().validate(json, key, modelProperty, next));
        }
        return result;
    }
    private ValidationMessage validateMinLength(String name, String value, Integer minLength) {
        if (minLength==null) return null;
        if (minLength > 0) {
            if (value.length() < minLength) {
                if (minLength == 1) {
                    return new ValidationMessage().message(name + " cannot be blank");
                }
                return new ValidationMessage().message(name + " must be at least " + minLength + " characters long");
            }
        }
        return null;
    }

    private ValidationMessage validateMaxLength(String name, String value, Integer maxLength) {
        if (maxLength==null) return null;
        if (maxLength > 0) {
            if (value.length() > maxLength) {
                return new ValidationMessage().message(name + " must be no more than " + maxLength + " characters long");
            }
        }
        return null;
    }

    private ValidationMessage validateMinMaxLength(String name, String value, Integer minLength, Integer maxLength) {
        if (minLength == null) {
            return validateMaxLength(name, value, maxLength);
        }
        else if (maxLength == null) {
            return validateMinLength(name, value, minLength);
        }
        if (value.length() < minLength || value.length() > maxLength) {
            if (minLength == 1) {
                return new ValidationMessage().message(name + " cannot be blank and cannot be longer than " + maxLength
                        + " characters long");
            }
            return new ValidationMessage().message(name + " must be at least " + minLength
                    + " characters long and no more than " + maxLength + " characters long");
        }
        return null;
    }

}
