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
package com.centurylink.mdw.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.model.JsonArray;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.util.file.FileHelper;

/**
 * Helper methods and access to JSON configuration values.
 * Takes into account PaaS vs File-based configurations.
 */
public class JsonUtil {

    public static final String read(String name) throws IOException {
        return read(name, JsonUtil.class.getClassLoader());
    }

    /**
     * Strips out comment lines (where first non-whitespace is //).
     * Does not support multi-line comments.
     */
    public static final String read(String name, ClassLoader classLoader) throws IOException {
        if (ApplicationContext.isCloudFoundry()) {
            return System.getenv("mdw_" + name.substring(0, name.lastIndexOf('.')));
        }
        else {
            InputStream stream = FileHelper.readFile(name, classLoader);
            if (stream == null)
                stream = FileHelper.readFile(name, classLoader);
            if (stream == null) {
                return null;
            }
            else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                try {
                    StringBuffer config = new StringBuffer();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.matches("^\\s*//.*$"))
                            config.append(line).append("\n");
                    }
                    return config.toString();
                }
                finally {
                    reader.close();
                }
            }
        }
    }

    public static final JSONObject getJson(Map<String,String> map) throws JSONException {
        if (map == null)
            return null;
        JSONObject jsonObj = new JsonObject();
        for (String key : map.keySet()) {
            String value = map.get(key);
            if (value != null)
                jsonObj.put(key, value);
        }
        return jsonObj;
    }

    public static final Map<String,String> getMap(JSONObject jsonObj) throws JSONException {
        Map<String,String> map = new HashMap<String,String>();
        String[] names =  JSONObject.getNames(jsonObj);
        if (names != null) {
            for (String name : names)
                map.put(name, jsonObj.getString(name));
        }
        return map;
    }

    private static final DateFormat utcDateTime = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
    static {
        utcDateTime.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    public static final String formatUtcDateTime(Date date) {
        return utcDateTime.format(date);
    }
    public static final Date parseUtcDateTime(String dt) throws java.text.ParseException {
        return utcDateTime.parse(dt);
    }

    public static Map<String,JSONObject> getJsonObjects(JSONObject json) throws JSONException {
        Map<String,JSONObject> objects = new HashMap<String,JSONObject>();
        Iterator<?> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next().toString();
            objects.put(key, json.getJSONObject(key));
        }
        return objects;
    }

    public static JsonArray getJsonArray(List<? extends Jsonable> jsonables) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Jsonable jsonable : jsonables) {
            jsonArray.put(jsonable.getJson());
        }
        return new JsonArray(jsonArray);
    }
}
