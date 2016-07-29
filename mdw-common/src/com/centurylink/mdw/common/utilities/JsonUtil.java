/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.ApplicationContext;

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
     *
     * TODO: support comments in PaaS properties
     */
    public static final String read(String name, ClassLoader classLoader) throws IOException {
        if (ApplicationContext.isPaaS()) {
            return System.getenv(name);
        }
        else {
            InputStream stream = FileHelper.readFile(name, classLoader);
            if (stream == null)
                stream = FileHelper.readFile(name + ".json", classLoader);
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
        JSONObject jsonObj = new JSONObject();
        for (String key : map.keySet()) {
            String value = map.get(key);
            if (value != null)
                jsonObj.put(key, value);
        }
        return jsonObj;
    }

    public static final Map<String,String> getMap(JSONObject jsonObj) throws JSONException {
        Map<String,String> map = new HashMap<String,String>();
        for (String name : JSONObject.getNames(jsonObj))
            map.put(name, jsonObj.getString(name));
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
}
