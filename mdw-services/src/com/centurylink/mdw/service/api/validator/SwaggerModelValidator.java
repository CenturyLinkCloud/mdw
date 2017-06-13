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

import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.service.api.MdwSwagger;

import io.limberest.api.validate.SwaggerRequest;
import io.limberest.api.validate.SwaggerValidator;
import io.limberest.service.ResourcePath;
import io.limberest.service.http.Request.HttpMethod;
import io.limberest.validate.Result;

/**
 * Validate JSON service requests against swagger annotations.
 */
public class SwaggerModelValidator extends SwaggerValidator {

    public SwaggerModelValidator(String method, String path) throws ValidationException {
        super(new SwaggerRequest(HttpMethod.valueOf(method.toUpperCase()),
                new ResourcePath('/' + path), MdwSwagger.getSwagger('/' + path)));
    }

    public void validate(String path, Query query, JSONObject body, Map<String, String> headers)
            throws ValidationException {
        validate(path, query, body, headers, false);
    }

    public void validate(String path, Query query, JSONObject body, Map<String, String> headers,
            boolean strict) throws ValidationException {
        Result result = new Result();
        try {
            result.also(super.validatePath(getSwaggerRequest().getPath(), strict));
            result.also(super.validateQuery(new io.limberest.service.Query(query.getFilters()), strict));
            result.also(super.validateHeaders(headers, strict));
            result.also(super.validateBody(body, strict));
            if (result.isError())
                throw new ValidationException(result);
        }
        catch (io.limberest.validate.ValidationException ex) {
            throw new ValidationException(ex.getCode(), ex.getMessage());
        }
    }

    public void validatePath(String path, boolean strict) throws ValidationException {
        try {
            Result result = super.validatePath(new ResourcePath(path), strict);
            if (result.isError())
                throw new ValidationException(result);
        }
        catch (io.limberest.validate.ValidationException ex) {
            throw new ValidationException(ex.getCode(), ex.getMessage());
        }
    }

    // TODO honor strict
    public void validateQuery(Query query, boolean strict) throws ValidationException {
        try {
            Result result = super.validateQuery(new io.limberest.service.Query(query.getFilters()), strict);
            if (result.isError())
                throw new ValidationException(result);
        }
        catch (io.limberest.validate.ValidationException ex) {
            throw new ValidationException(ex.getCode(), ex.getMessage());
        }
    }

    public void validateRequestHeaders(Map<String,String> headers, boolean strict) throws ValidationException {
        try {
            Result result = super.validateHeaders(headers, strict);
            if (result.isError())
                throw new ValidationException(result);
        }
        catch (io.limberest.validate.ValidationException ex) {
            throw new ValidationException(ex.getCode(), ex.getMessage());
        }
    }

    public void validateRequestBody(JSONObject body, boolean strict) throws ValidationException {
        try {
            Result result = super.validateBody(body, strict);
            if (result.isError())
                throw new ValidationException(result);
        }
        catch (io.limberest.validate.ValidationException ex) {
            throw new ValidationException(ex.getCode(), ex.getMessage());
        }
    }



    // everything below is for compatibility

    /**
     * @deprecated Only for compatibility.
     */
    @Deprecated
    public SwaggerModelValidator() {
        this(true);
    }

    private CompatibleValidator compatibleValidator;
    @Deprecated
    public List<Validator> getValidators() { return compatibleValidator.getValidators(); }

    /**
     * @deprecated Do NOT specify compatibility.
     */
    @Deprecated
    public SwaggerModelValidator(boolean compatibility) {
        super((SwaggerRequest)null);
        if (compatibility)
            compatibleValidator = new CompatibleValidator();
    }

    /**
     * @deprecated use {@link #g
     */
    @Deprecated
    public void addValidator(Validator newValidator) {
        compatibleValidator.addValidator(newValidator);
    }


    /**
     * @deprecated use one of the specify validate methods for headers, body, etc
     */
    @Deprecated
    public ValidationResult validateModel(JSONObject json, Class<?> type, boolean strict)
            throws ValidationException, JSONException {
        return compatibleValidator.validateModel(json, type, strict);
    }

}
