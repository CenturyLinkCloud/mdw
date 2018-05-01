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
package com.centurylink.mdw.model.asset;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Represents a configured asset request mapping.
 * Example: request mapping for launching a process flow via REST.
 */
public class AssetRequest {

    public static enum HttpMethod {
        GET,
        POST,
        PUT,
        PATCH,
        DELETE
    }

    public static enum ParameterType {
        Path,
        Body,
        Query,
        Header,
        Form
    }

    /**
     * Asset path designation.
     */
    private String asset;
    public String getAsset() { return asset; }

    private HttpMethod method;
    public HttpMethod getMethod() { return method; }

    private String path;
    public String getPath() { return path; }

    private List<Parameter> parameters;
    public List<Parameter> getParameters() { return parameters; }
    public Parameter getParameter(String name) {
        if (parameters != null) {
            for (Parameter param : parameters) {
                if (param.name.equals(name))
                    return param;
            }
        }
        return null;
    }
    public void addParameter(Parameter parameter) {
        if (parameters == null) {
            parameters = new ArrayList<>();
        }
        else {
            removeParameter(parameter.name);
        }
        parameters.add(parameter);
    }
    public void removeParameter(String name) {
        if (parameters != null) {
            Parameter found = null;
            for (Parameter param : parameters) {
                if (param.name.equals(name)) {
                    found = param;
                    break;
                }
            }
            if (found != null)
                parameters.remove(found);
        }
    }
    public Parameter getBodyParameter() {
        if (parameters != null) {
            for (Parameter param : parameters) {
                if (param.getType() == ParameterType.Body)
                    return param;
            }
        }
        return null;
    }

    public AssetRequest(String asset, HttpMethod method, String path) {
        this(asset, method, path, (List<Parameter>)null);
    }

    public AssetRequest(String asset, HttpMethod method, String path, List<Parameter> parameters) {
        this.asset = asset;
        this.method = method;
        this.path = path;
        this.parameters = parameters;
    }

    public AssetRequest(String asset, HttpMethod method, String path, JSONArray parameters) {
        this.asset = asset;
        this.method = method;
        this.path = path;
        if (parameters != null) {
            for (int i = 0; i < parameters.length(); i++) {
                JSONArray param = parameters.getJSONArray(i);
                addParameter(new Parameter(param));
            }
        }
    }

    public class Parameter {

        private String name;
        public String getName() { return name; }

        private ParameterType type;
        public ParameterType getType() { return type; }

        public Parameter(String name, ParameterType type) {
            this.name = name;
            this.type = type;
        }

        /**
         * For parsing attribute array value (order-dependent).
         */
        public Parameter(JSONArray jsonArray) {
            if (jsonArray.length() < 2)
                throw new JSONException("Missing required values: 'name', 'type'");
            this.name = jsonArray.getString(0);
            this.type = ParameterType.valueOf(jsonArray.getString(1));
            for (int i = 2; i < jsonArray.length(); i++) {
                switch (i) {
                  case 2: this.required = jsonArray.getBoolean(i);
                          break;
                  case 3: this.description = jsonArray.getString(i);
                          break;
                  case 4: this.dataType = jsonArray.getString(i);
                }
            }
        }

        private boolean required;
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }

        private String description;
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        /**
         * Can be an asset path designation.
         */
        private String dataType;
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }

    }
}
