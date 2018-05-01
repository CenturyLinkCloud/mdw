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
package com.centurylink.mdw.model.variable;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import com.centurylink.mdw.translator.JsonTranslator;
import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.translator.VariableTranslator;

/**
 * Shared attributes with standardized default naming.
 */
public class ServiceValuesAccess {

    public static final String REQUEST_VARIABLE = "Request Variable";
    public static final String REQUEST_HEADERS_VARIABLE = "Request Headers Variable";
    public static final String RESPONSE_VARIABLE = "Response Variable";
    public static final String RESPONSE_HEADERS_VARIABLE = "Response Headers Variable";

    private ProcessRuntimeContext context;

    public ServiceValuesAccess(ProcessRuntimeContext context) {
        this.context = context;
    }

    public Object getRequest() {
        return context.getVariables().get(getRequestVariableName());
    }

    /**
     * Most usages do not provide a way to set the REQUEST_VARIABLE attribute.
     * For these cases, a custom .impl asset would be needed to configure through
     * Designer or MDWHub.
     */
    public String getRequestVariableName() {
        return context.getAttribute(REQUEST_VARIABLE, "request");
    }

    @SuppressWarnings("unchecked")
    public Map<String,String> getRequestHeaders() {
        return (Map<String,String>)context.getVariables().get(getRequestHeadersVariableName());
    }

    /**
     * Most usages do not provide a way to set the REQUEST_HEADERS_VARIABLE attribute.
     * For these cases, a custom .impl asset would be needed to configure through
     * Designer or MDWHub.
     */
    public String getRequestHeadersVariableName() {
        return context.getAttribute(REQUEST_HEADERS_VARIABLE, "requestHeaders");
    }

    public Object getResponse() {
        return context.getVariables().get(getResponseVariableName());
    }

    /**
     * Most usages do not provide a way to set the RESPONSE_VARIABLE attribute.
     * For these cases, a custom .impl asset would be needed to configure through
     * Designer or MDWHub.
     */
    public String getResponseVariableName() {
        return context.getAttribute(RESPONSE_VARIABLE, "response");
    }

    @SuppressWarnings("unchecked")
    public Map<String,String> getResponseHeaders() {
        return (Map<String,String>)context.getVariables().get(getResponseHeadersVariableName());
    }

    /**
     * Most usages do not provide a way to set the RESPONSE_HEADERS_VARIABLE attribute.
     * For these cases, a custom .impl asset would be needed to configure through
     * Designer or MDWHub.
     */
    public String getResponseHeadersVariableName() {
        return context.getAttribute(RESPONSE_HEADERS_VARIABLE, "responseHeaders");
    }

    public String getHttpMethod() {
        Map<String,String> requestHeaders = getRequestHeaders();
        if (requestHeaders == null)
            return null;
        return requestHeaders.get(Listener.METAINFO_HTTP_METHOD);
    }

    /**
     * Always begins with "/".
     */
    public String getRequestPath() {
        Map<String,String> requestHeaders = getRequestHeaders();
        if (requestHeaders == null)
            return null;
        String path = requestHeaders.get(Listener.METAINFO_REQUEST_PATH);
        return path.startsWith("/") ? path : "/" + path;
    }

    public Query getQuery() {
        return new Query(getRequestPath(), getParameters(getRequestHeaders()));
    }

    protected Map<String,String> getParameters(Map<String,String> headers) {
        Map<String,String> params = new HashMap<String,String>();
        String query = headers.get(Listener.METAINFO_REQUEST_QUERY_STRING);
        if (query == null)
            query = headers.get("request-query-string");
        if (query != null && !query.isEmpty()) {
            for (String pair : query.split("&")) {
                int idx = pair.indexOf("=");
                try {
                    params.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                            URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
                catch (UnsupportedEncodingException ex) {
                    // as if UTF-8 is going to be unsupported
                }
            }
        }
        return params;
    }

    public JSONObject toJson(String variableName, Object objectValue) throws TranslationException {
        if (objectValue == null)
            return null;
        Variable variable = context.getProcess().getVariable(variableName);
        com.centurylink.mdw.variable.VariableTranslator translator = VariableTranslator
                .getTranslator(context.getPackage(), variable.getType());
        if (translator instanceof JsonTranslator) {
            return ((JsonTranslator)translator).toJson(objectValue);
        }
        else {
            throw new TranslationException("Cannot convert to JSON using " + translator.getClass());
        }
    }

    /**
     * For Jsonable, _type property is required.
     */
    public Object fromJson(String variableName, JSONObject json) throws TranslationException {
        if (json == null)
            return null;
        Variable variable = context.getProcess().getVariable(variableName);
        com.centurylink.mdw.variable.VariableTranslator translator = VariableTranslator
                .getTranslator(context.getPackage(), variable.getType());
        if (translator instanceof JsonTranslator) {
            return ((JsonTranslator)translator).fromJson(json);
        }
        else {
            throw new TranslationException("Cannot convert from JSON using " + translator.getClass());
        }
    }

}
