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
package com.centurylink.mdw.common.service;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Lightweight action request especially for JSON.
 */
public class ActionRequest implements Jsonable {

    private String action;
    private Map<String,String> parameters;

    private Map<String,JSONObject> jsonObjects;

    public ActionRequest(String action, Map<String,String> parameters) {
        this.action = action;
        this.parameters = parameters;
    }

    public ActionRequest(JSONObject json) throws JSONException {
        JSONObject actionJson = json.getJSONObject("action");
        action = actionJson.getString("name");
        if (json.has("parameters")) {
            parameters = new HashMap<String,String>();
            JSONObject parametersJson = json.getJSONObject("parameters");
            if (parametersJson != null) {
                String[] names = JSONObject.getNames(parametersJson);
                if (names != null) {
                    for (String name : names)
                       parameters.put(name, parametersJson.getString(name));
                }
            }
        }
        String[] names = JSONObject.getNames(json);
        if (names != null) {
            for (String jsonName : names) {
                if (!jsonName.equals("action") && !jsonName.equals("parameters"))
                    addJson(jsonName, json.getJSONObject(jsonName));
            }
        }
    }

    public void addJson(String jsonName, JSONObject jsonObject) {
        if (jsonObjects == null)
            jsonObjects = new HashMap<String,JSONObject>();
        jsonObjects.put(jsonName, jsonObject);
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject actionJson = new JSONObject();
        actionJson.put("name", action);
        json.put("action", actionJson);
        if (parameters != null) {
            JSONObject paramsJson = new JSONObject();
            for (String name : parameters.keySet())
                paramsJson.put(name, parameters.get(name));
            actionJson.put("parameters", paramsJson);
        }
        if (jsonObjects != null) {
            for (String jsonName : jsonObjects.keySet())
                json.put(jsonName, jsonObjects.get(jsonName));
        }

        return json;
    }

    public String getJsonName() {
        return "actionRequest";
    }

}
