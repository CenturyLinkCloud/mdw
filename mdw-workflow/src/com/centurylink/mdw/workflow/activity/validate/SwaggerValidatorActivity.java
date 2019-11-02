/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.workflow.activity.validate;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.StatusResponse;
import com.centurylink.mdw.model.asset.AssetRequest;
import com.centurylink.mdw.model.asset.AssetRequest.ParameterType;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.variable.ServiceValuesAccess;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.service.api.MdwSwaggerCache;
import com.centurylink.mdw.service.api.SwaggerModelValidator;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import io.limberest.service.http.Status;
import io.limberest.validate.Result;
import io.limberest.validate.ValidationException;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tracked(LogLevel.TRACE)
@Activity(value="Swagger Validator", icon="com.centurylink.mdw.base/swagger.png",
        pagelet="com.centurylink.mdw.base/swaggerValidator.pagelet")
public class SwaggerValidatorActivity extends DefaultActivityImpl {

    public static final String VALIDATE = "Validate";
    public static final String STRICT = "Strict";
    public static final String PATH = "Path";

    @Override
    public boolean isDisabled() throws ActivityException {
        boolean disabled = super.isDisabled();
        if (disabled)
            setReturnCode("true");
        return disabled;
    }

    @Override
    public Object execute(ActivityRuntimeContext runtimeContext) throws ActivityException {
        ServiceValuesAccess serviceValues = runtimeContext.getServiceValues();
        Map<String,String> requestHeaders = serviceValues.getRequestHeaders();
        if (requestHeaders == null)
            throw new ActivityException("Missing request headers: " + serviceValues.getRequestHeadersVariableName());

        String httpMethod = serviceValues.getHttpMethod();
        String requestPath = getRequestPath(runtimeContext);
        if (requestPath == null)
            throw new ActivityException("Request path not found");

        String lookupPath = requestPath.replaceAll("\\{.*}", "");
        while (lookupPath.endsWith(("/")))
            lookupPath = lookupPath.substring(0, lookupPath.length() - 1);
        logDebug("Swagger validation for lookupPath=" + lookupPath + " and requestPath=" + requestPath);

        Object request = null;
        if (!"GET".equalsIgnoreCase(httpMethod)) {
            request = serviceValues.getRequest();
        }

        try {
            Swagger swagger = MdwSwaggerCache.getSwagger(lookupPath);
            Path swaggerPath = swagger.getPath(requestPath);
            if (swaggerPath == null)
                throw new ValidationException(Status.NOT_FOUND.getCode(), "No swagger found: " + requestPath);
            Operation swaggerOp = swaggerPath.getOperationMap().get(HttpMethod.valueOf(httpMethod));
            if (swaggerOp == null)
                throw new ValidationException(Status.NOT_FOUND.getCode(), "No swagger found: " + httpMethod + " " + requestPath);
            SwaggerModelValidator validator = new SwaggerModelValidator(httpMethod, requestPath, swagger);
            Result result = new Result();
            if (isValidate(ParameterType.Path))
                result.also(validator.validatePath(requestPath, isStrict()));
            if (isValidate(ParameterType.Query))
                result.also(validator.validateQuery(serviceValues.getQuery(), isStrict()));
            if (isValidate(ParameterType.Header))
                result.also(validator.validateHeaders(requestHeaders, isStrict()));
            if (isValidate(ParameterType.Body)) {
                if (request == null) {
                    result.also(Status.BAD_REQUEST, "Missing request: " + serviceValues.getRequestVariableName());
                }
                else {
                    JSONObject requestJson = getRequestJson(request, runtimeContext);
                    result.also(validator.validateBody(requestJson, isStrict()));
                }
            }
            return handleResult(result);
        }
        catch (ValidationException ex) {
            logger.debugException(ex.getMessage(), ex);
            return handleResult(ex.getResult());
        }
    }

    public String getRequestPath(ActivityRuntimeContext runtimeContext) throws ActivityException {
        String requestPath = getAttribute(PATH);
        if (requestPath == null) {
            // try process-defined request path
            AssetRequest processRequest = getProcessDefinition().getRequest();
            if (processRequest != null) {
                String processPath = processRequest.getPath();
                if (!processPath.startsWith("/"))
                    processPath = "/" + processPath;
                requestPath = "/" + getPackage().getName().replace('.', '/') + processPath;
            }
        }
        if (requestPath == null) {
            // fallback is actual request path
            requestPath = runtimeContext.getServiceValues().getRequestPath();
        }
        return requestPath;
    }

    public JSONObject getRequestJson(Object request, ActivityRuntimeContext runtimeContext) throws ActivityException {
        ServiceValuesAccess serviceValues = runtimeContext.getServiceValues();
        return serviceValues.toJson(serviceValues.getRequestVariableName(), request);
    }

    /**
     * Populates "response" variable with a default JSON status object.  Can be overwritten by
     * custom logic in a downstream activity, or in a JsonRestService implementation.
     */
    protected Object handleResult(Result result) throws ActivityException {
        ServiceValuesAccess serviceValues = getRuntimeContext().getServiceValues();
        StatusResponse statusResponse;
        if (result.isError()) {
            logError("Validation error: " + result.getStatus().toString());
            statusResponse = new StatusResponse(result.getWorstCode(), result.getStatus().getMessage());
            String responseHeadersVarName = serviceValues.getResponseHeadersVariableName();
            Map<String,String> responseHeaders = serviceValues.getResponseHeaders();
            if (responseHeaders == null) {
                Variable responseHeadersVar = getMainProcessDefinition().getVariable(responseHeadersVarName);
                if (responseHeadersVar == null)
                    throw new ActivityException("Missing response headers variable: " + responseHeadersVarName);
                responseHeaders = new HashMap<>();
            }
            responseHeaders.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(statusResponse.getStatus().getCode()));
            setVariableValue(responseHeadersVarName, responseHeaders);
        }
        else {
            statusResponse = new StatusResponse(com.centurylink.mdw.model.Status.OK, "Valid request");
        }
        String responseVariableName = serviceValues.getResponseVariableName();
        Variable responseVariable = getMainProcessDefinition().getVariable(responseVariableName);
        if (responseVariable == null)
            throw new ActivityException("Missing response variable: " + responseVariableName);
        Object responseObject;
        if (responseVariable.getType().equals(Jsonable.class.getName()))
            responseObject = statusResponse; // _type has not been set, so serialization would fail
        else
            responseObject = serviceValues.fromJson(responseVariableName, statusResponse.getJson());
        setVariableValue(responseVariableName, responseObject);
        return !result.isError();
    }

    /**
     * Strict means unexpected elements cause validation failure (vs ignored).
     */
    protected boolean isStrict() {
        return getAttribute(STRICT, false);
    }

    private List<String> validateParams;
    protected boolean isValidate(ParameterType paramType) {
        if (validateParams == null) {
            validateParams = new ArrayList<String>();
            String attr = getAttribute(VALIDATE, "");
            if (!attr.isEmpty()) {
                JSONArray arr = new JSONArray(attr);
                for (int i = 0; i < arr.length(); i++) {
                    validateParams.add(arr.getString(i));
                }
            }
        }
        return validateParams.contains(paramType.toString());
    }
}
