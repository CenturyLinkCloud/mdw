/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */

package com.centurylink.mdw.service.api.validator;

import java.util.Iterator;

import org.json.JSONObject;

import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;

/**
 * Dynamic Java workflow asset.
 */
public class ArrayValidator implements Validator {
    @Override
    public ValidationResult validate(JSONObject json, String key, Property modelProperty,
            Iterator<Validator> next) {
        ValidationResult result = new ValidationResult();
        if (modelProperty instanceof ArrayProperty) {
            /**
             * TBD
            */
        }
        if(next.hasNext()) {
            result.addValidationMessages(next.next().validate(json, key, modelProperty, next));
        }
        return result;
    }

}
