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
package com.centurylink.mdw.model.attribute;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.util.JsonUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class Attribute implements Comparable<Attribute> {

    public Attribute(){
    }

    public Attribute(String name, String value) {
        this.name = name;
        this.value = value;
    }

    private String name;
    public String getName() {
        return name;
    }

    private String value;
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

    private String group;
    public String getGroup() {
        return group;
    }
    public void setGroup(String group) {
        this.group = group;
    }

    public static String findAttribute(List<Attribute> attrs, String name) {
        if (attrs==null) return null;
        for (Attribute attr : attrs) {
            if (name.equals(attr.getName()))
                return attr.getValue();
        }
        return null;
    }

    /**
     * Set the value of a process attribute.
     * If the value is null, the attribute is removed.
     * If the attribute does not exist and the value is not null, the attribute is created.
     * @param name attribute name
     * @param value value to be set
     * TODO: attributes should be a map
     */
    public static void setAttribute(List<Attribute> attrs, String name, String value) {
        Attribute found = null;
        for (Attribute attr : attrs) {
            if (name.equals(attr.getName())) {
                found = attr;
                break;
            }
        }
        if (value == null) {
            if (found != null)
                attrs.remove(found);
        }
        else {
            if (found != null)
                found.setValue(value);
            else
                attrs.add(new Attribute(name, value));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Attribute))
            return false;
        Attribute other = (Attribute) o;

        if (getName() == null)
            return other.getName() == null;
        else
            return getName().equals(other.getName());
    }

    @Override
    public int hashCode() {
        return getName() == null ? super.hashCode() : getName().hashCode();
    }

    public int compareTo(Attribute other) {
        if (other == null || other.getName() == null)
            return 1;
        if (this.getName() == null)
            return -1;

        return this.getName().compareTo(other.getName());
    }

    public static List<Attribute> getAttributes(JSONObject attributesJson) throws JSONException {
        return getAttributes(null, attributesJson);
    }

    public static List<Attribute> getAttributes(String group, JSONObject attributesJson) throws JSONException {
        if (attributesJson == null)
            return null;
        List<Attribute> attributes = new ArrayList<>();
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
                        Attribute attr = new Attribute(groupKey, groupJson.getString(groupKey));
                        attr.setGroup(key);
                        attributes.add(attr);
                    }
                }
                else {
                    // ungrouped attribute
                    attributes.add(new Attribute(key, (String)value));
                }
            }
        }
        else {
            JSONObject groupJson = attributesJson.getJSONObject(group);
            Iterator<?> keys = groupJson.keys();
            while (keys.hasNext()) {
                String key = keys.next().toString();
                Attribute attr = new Attribute(key, groupJson.getString(key));
                attr.setGroup(group);
                attributes.add(attr);
            }
        }
        return attributes;
    }

    public static List<Attribute> getAttributes(Map<String,Object> yaml) {
        if (yaml == null)
            return null;
        List<Attribute> attributes = new ArrayList<>();
        for (String name : yaml.keySet()) {
            attributes.add(new Attribute(name, (String)yaml.get(name)));
        }
        return attributes;
    }

    public static Map<String,Object> getAttributesYaml(List<Attribute> attributes) {
        Map<String,Object> yaml = new TreeMap<>(); // sorted
        for (Attribute attr : attributes) {
            if (!WorkAttributeConstant.LOGICAL_ID.equals(attr.getName()) && attr.getValue() != null)
                yaml.put(attr.getName(), attr.getValue());
        }
        return yaml;
    }

    public static JSONObject getAttributesJson(List<Attribute> attributes) throws JSONException {
        return getAttributesJson(attributes, false);
    }

    public static JSONObject getAttributesJson(List<Attribute> attributes, boolean grouped) throws JSONException {
        if (attributes == null)
            return null;
        JSONObject attrsJson = new JsonObject();
        boolean hasSla = false;
        if (grouped) {
            Map<String,List<Attribute>> byGroup = new HashMap<>();
            for (Attribute attr : attributes) {
                List<Attribute> groupAttrs = byGroup.get(attr.getGroup());
                if (groupAttrs == null) {
                    groupAttrs = new ArrayList<>();
                    byGroup.put(attr.getGroup(), groupAttrs);
                }
                groupAttrs.add(attr);
            }
            for (String group : byGroup.keySet()) {
                if (group == null) {
                    for (Attribute ungroupedAttr : byGroup.get(group))
                        attrsJson.put(ungroupedAttr.getName(), ungroupedAttr.getValue());
                }
                else {
                    JSONObject groupJson = new JsonObject();
                    attrsJson.put(group, groupJson);
                    for (Attribute groupedAttr : byGroup.get(group))
                        groupJson.put(groupedAttr.getName(), groupedAttr.getValue());
                }
            }
        }
        else {
            for (Attribute attr : attributes) {
                if (WorkAttributeConstant.SLA.equals(attr.getName())) {
                    // special handling to avoid adding zero SLAs
                    if (attr.getValue() != null && !attr.getValue().equals("0")) {
                        hasSla = true;
                        attrsJson.put(attr.getName(), attr.getValue());
                    }
                }
                else {
                    // don't add empty attributes or zero slas or logical ids (which are stored as "id" field)
                    if (!WorkAttributeConstant.LOGICAL_ID.equals(attr.getName()) && attr.getValue() != null)
                        attrsJson.put(attr.getName(), attr.getValue());
                }
            }
            if (!hasSla && attrsJson.has(WorkAttributeConstant.SLA_UNIT))
                attrsJson.remove(WorkAttributeConstant.SLA_UNIT);
        }
        return attrsJson;
    }

    public static List<String> parseList(String value) {
        List<String> list = new ArrayList<>();
        if (value.startsWith("[")) {
            JSONArray jsonArr = new JSONArray(value);
            for (int i = 0; i < jsonArr.length(); i++)
                list.add(jsonArr.getString(i));
        } else {
            StringTokenizer st = new StringTokenizer(value, "#");
            while (st.hasMoreTokens())
                list.add(st.nextToken());
        }
        return list;
    }

    public static Map<String,String> parseMap(String map) {
        HashMap<String, String> hash = new LinkedHashMap<>();
        if (map != null) {
            if (map.startsWith("{")) {
                return JsonUtil.getMap(new JsonObject(map));
            } else {
                int name_start = 0;
                int n = map.length();
                int m;
                while (name_start < n) {
                    m = name_start;
                    char ch = map.charAt(m);
                    while (ch != '=' && ch != ';' && m < n - 1) {
                        m++;
                        ch = map.charAt(m);
                    }
                    if (ch == '=') {
                        int value_start = m + 1;
                        boolean escaped = false;
                        for (m = value_start; m < n; m++) {
                            if (escaped) escaped = false;
                            else {
                                ch = map.charAt(m);
                                if (ch == '\\') escaped = true;
                                else if (ch == ';') break;
                            }
                        }
                        hash.put(map.substring(name_start, value_start - 1).trim(),
                                map.substring(value_start, m).trim());
                        name_start = m + 1;
                    } else if (ch == ';') {
                        if (m > name_start) {
                            hash.put(map.substring(name_start, m).trim(), null);
                        }
                        name_start = m + 1;
                    } else {    // m == n-1
                        if (m > name_start) {
                            hash.put(map.substring(name_start, m).trim(), null);
                        }
                        name_start = m + 1;
                    }
                }
            }
        }
        return hash;
    }

    public static List<String[]> parseTable(String string,
            char field_delimiter, char row_delimiter, int columnCount) {
        List<String[]> table = new ArrayList<>();
        if (string != null) {
            if (string.startsWith("[")) {
                List<String[]> rows = new ArrayList<>();
                JSONArray outer = new JSONArray(string);
                for (int i = 0; i < outer.length(); i++) {
                    String[] row = new String[columnCount];
                    JSONArray inner = outer.getJSONArray(i);
                    for (int j = 0; j < row.length; j++) {
                        if (inner.length() > j)
                            row[j] = inner.getString(j);
                        else
                            row[j] = "";
                    }
                    rows.add(row);
                }
                return rows;
            } else {
                int row_start = 0;
                int field_start;
                int n = string.length();
                String[] row;
                int m, j;
                StringBuffer sb;
                while (row_start < n) {
                    row = new String[columnCount];
                    table.add(row);
                    j = 0;
                    field_start = row_start;
                    char ch = field_delimiter;
                    while (ch == field_delimiter) {
                        sb = new StringBuffer();
                        boolean escaped = false;
                        for (m = field_start; m < n; m++) {
                            ch = string.charAt(m);
                            if (ch == '\\' && !escaped) {
                                escaped = true;
                            } else {
                                if (!escaped && (ch == field_delimiter || ch == row_delimiter)) {
                                    break;
                                } else {
                                    sb.append(ch);
                                    escaped = false;
                                }
                            }
                        }
                        if (j < columnCount)
                            row[j] = sb.toString();
                        if (m >= n || ch == row_delimiter) {
                            row_start = m + 1;
                            break;
                        } else {  // ch==field_delimiter
                            field_start = m + 1;
                            j++;
                        }
                    }
                }
            }
        }
        return table;
    }
}
