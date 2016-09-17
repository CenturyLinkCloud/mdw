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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.value.attribute.AttributeVO;

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

    public static JSONObject getAttributesJson(List<AttributeVO> attributes) throws JSONException {
        return getAttributesJson(attributes, false);
    }

    public static JSONObject getAttributesJson(List<AttributeVO> attributes, boolean grouped) throws JSONException {
        if (attributes == null)
            return null;
        JSONObject attrsJson = new JSONObject();
        boolean hasSla = false;
        if (grouped) {
            Map<String,List<AttributeVO>> byGroup = new HashMap<String,List<AttributeVO>>();
            for (AttributeVO attr : attributes) {
                List<AttributeVO> groupAttrs = byGroup.get(attr.getAttributeGroup());
                if (groupAttrs == null) {
                    groupAttrs = new ArrayList<AttributeVO>();
                    byGroup.put(attr.getAttributeGroup(), groupAttrs);
                }
                groupAttrs.add(attr);
            }
            for (String group : byGroup.keySet()) {
                if (group == null) {
                    for (AttributeVO ungroupedAttr : byGroup.get(group))
                        attrsJson.put(ungroupedAttr.getAttributeName(), ungroupedAttr.getAttributeValue());
                }
                else {
                    JSONObject groupJson = new JSONObject();
                    attrsJson.put(group, groupJson);
                    for (AttributeVO groupedAttr : byGroup.get(group))
                        groupJson.put(groupedAttr.getAttributeName(), groupedAttr.getAttributeValue());
                }
            }
        }
        else {
            for (AttributeVO attr : attributes) {
                if (WorkAttributeConstant.SLA.equals(attr.getAttributeName())) {
                    // special handling to avoid adding zero SLAs
                    if (attr.getAttributeValue() != null && !attr.getAttributeValue().equals("0")) {
                        hasSla = true;
                        attrsJson.put(attr.getAttributeName(), attr.getAttributeValue());
                    }
                }
                else {
                    // don't add empty attributes or zero slas or logical ids (which are stored as "id" field)
                    if (!WorkAttributeConstant.LOGICAL_ID.equals(attr.getAttributeName()) && attr.getAttributeValue() != null)
                        attrsJson.put(attr.getAttributeName(), attr.getAttributeValue());
                }
            }
            if (!hasSla && attrsJson.has(WorkAttributeConstant.SLA_UNIT))
                attrsJson.remove(WorkAttributeConstant.SLA_UNIT);
        }
        return attrsJson;
    }

    public static List<AttributeVO> getAttributes(JSONObject attributesJson) throws JSONException {
        return getAttributes(null, attributesJson);
    }

    public static List<AttributeVO> getAttributes(String group, JSONObject attributesJson) throws JSONException {
        if (attributesJson == null)
            return null;
        List<AttributeVO> attributes = new ArrayList<AttributeVO>();
        if (group == null) {
            Iterator<?> keys = attributesJson.keys();
            while (keys.hasNext()) {
                String key = keys.next().toString();
                Object value = attributesJson.get(key);
                if (value instanceof JSONObject) {
                    JSONObject groupJson = (JSONObject)value;
                    Iterator<?> groupKeys = groupJson.keys();
                    while (groupKeys.hasNext()) {
                        String groupKey = groupKeys.next().toString();
                        AttributeVO attr = new AttributeVO(groupKey, groupJson.getString(groupKey));
                        attr.setAttributeGroup(groupKey);
                        attributes.add(attr);
                    }
                }
                else {
                    // ungrouped attribute
                    attributes.add(new AttributeVO(key, (String)value));
                }
            }
        }
        else {
            JSONObject groupJson = attributesJson.getJSONObject(group);
            Iterator<?> keys = groupJson.keys();
            while (keys.hasNext()) {
                String key = keys.next().toString();
                AttributeVO attr = new AttributeVO(key, groupJson.getString(key));
                attr.setAttributeGroup(group);
                attributes.add(attr);
            }
        }
        return attributes;
    }

    public static String padLogicalId(String logicalId) {
        char prefix = logicalId.charAt(0);
        String padded = logicalId.substring(1);
        while (padded.length() < 3)
            padded = "0" + padded;
        return prefix + padded;
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
}
