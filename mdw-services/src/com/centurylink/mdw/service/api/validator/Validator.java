/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */

package com.centurylink.mdw.service.api.validator;

import java.util.Iterator;

import org.json.JSONObject;

import io.swagger.models.properties.Property;

/**
 * Dynamic Java workflow asset.
 */
public interface Validator {
    ValidationResult validate(JSONObject json, String key, Property modelProperty, Iterator<Validator> next);
}