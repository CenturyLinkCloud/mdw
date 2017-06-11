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
package com.centurylink.mdw.model;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A Jsonable that contains a generic JSON array.
 */
public class JsonArray implements Jsonable {

    public static final String GENERIC_ARRAY = "genericArray";

    private JSONArray array;
    public JSONArray getArray() { return array; }

    public JsonArray(JSONArray array) {
        this.array = array;
    }

    public JsonArray(List<String> values) {
        array = new JSONArray(values);
    }

    public List<String> getList() throws JSONException {
        List<String> list = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getString(i));
            }
        }
        return list;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject jsonObj = create();
        // either array or values should be populated
        if (array != null)
            jsonObj.put(getJsonName(), array);
        return jsonObj;
    }

    public String getJsonName() {
        return GENERIC_ARRAY;
    }
}
