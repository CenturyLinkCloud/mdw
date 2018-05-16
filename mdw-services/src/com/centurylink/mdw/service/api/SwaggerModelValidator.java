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
package com.centurylink.mdw.service.api;

import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.common.service.Query;

import io.limberest.api.validate.SwaggerRequest;
import io.limberest.api.validate.SwaggerValidator;
import io.limberest.service.ResourcePath;
import io.limberest.service.http.Request.HttpMethod;
import io.limberest.validate.Result;
import io.limberest.validate.ValidationException;
import io.swagger.models.Swagger;

/**
 * Validate JSON service requests against swagger annotations.
 */
public class SwaggerModelValidator extends SwaggerValidator {

    public SwaggerModelValidator(String method, String path) throws ValidationException {
        this(method, path, MdwSwaggerCache.getSwagger(path.startsWith("/") ? path : '/' + path));
    }

    public SwaggerModelValidator(String method, String path, Swagger swagger) throws ValidationException {
        super(new SwaggerRequest(HttpMethod.valueOf(method.toUpperCase()), new ResourcePath(path), swagger));
    }

    public Result validate(String path, Query query, JSONObject body, Map<String,String> headers)
            throws ValidationException {
        return validate(path, query, body, headers, false);
    }

    public Result validate(String path, Query query, JSONObject body, Map<String,String> headers,
            boolean strict) throws ValidationException {
        Result result = new Result();
        result.also(super.validatePath(getSwaggerRequest().getPath(), strict));
        result.also(super.validateQuery(new io.limberest.service.Query(query.getFilters()), strict));
        result.also(super.validateHeaders(headers, strict));
        result.also(super.validateBody(body, strict));
        return result;
    }

    public Result validatePath(String path, boolean strict) throws ValidationException {
        return super.validatePath(new ResourcePath(path), strict);
    }

    public Result validateQuery(Query query, boolean strict) throws ValidationException {
        return super.validateQuery(new io.limberest.service.Query(query.getFilters()), strict);
    }
}
