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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.JsonObject;

public class Attribute implements Comparable<Attribute> {

    private Long id;
    private String name;
    private String value;
    private String group;

    public Attribute(){
    }

    public Attribute(String name, String value) {
        this.id = null;
        this.name = name;
        this.value = value;
    }

    public Attribute(Long id, String name, String value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public Long getAttributeId() {
        return id;
    }

    public void setAttributeId(Long id) {
        this.id = id;
    }

    public String getAttributeName() {
        return name;
    }

    public void setAttributeName(String name) {
        this.name = name;
    }

    public String getAttributeValue() {
        return value;
    }

    public void setAttributeValue(String value) {
        this.value = value;
    }

    public String getAttributeGroup() {
        return group;
    }

    public void setAttributeGroup(String group) {
        this.group = group;
    }

    public static String findAttribute(List<Attribute> attrs, String name) {
        if (attrs==null) return null;
        for (Attribute attr : attrs) {
            if (name.equals(attr.getAttributeName()))
                return attr.getAttributeValue();
        }
        return null;
    }

    /**
     * Set the value of a process attribute.
     * If the value is null, the attribute is removed.
     * If the attribute does not exist and the value is not null, the attribute
     * is created.
     * @param name attribute name
     * @param v value to be set. When it is null, the attribute is removed
     */
    public static void setAttribute(List<Attribute> attrs, String name, String v) {
        for (Attribute attr : attrs) {
            if (name.equals(attr.getAttributeName())) {
                if (v!=null) attr.setAttributeValue(v);
                else attrs.remove(attr);  // TODO this will throw a concurrent modification exception
                return;
            }
        }
        if (v!=null) {
            Attribute attr = new Attribute(null, name, v);
            // TODO: need to retire attribute type concept
            attrs.add(attr);
        }
    }

    public static void removeAttribute(List<Attribute> attrs, String name) {
        setAttribute(attrs, name, null);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Attribute))
            return false;
        Attribute other = (Attribute) o;

        if (getAttributeName() == null)
            return other.getAttributeName() == null;
        else
            return getAttributeName().equals(other.getAttributeName());
    }

    public int compareTo(Attribute other) {
        if (other == null || other.getAttributeName() == null)
            return 1;
        if (this.getAttributeName() == null)
            return -1;

        return this.getAttributeName().compareTo(other.getAttributeName());
    }

    public static List<Attribute> getAttributes(JSONObject attributesJson) throws JSONException {
        return getAttributes(null, attributesJson);
    }

    public static List<Attribute> getAttributes(String group, JSONObject attributesJson) throws JSONException {
        if (attributesJson == null)
            return null;
        List<Attribute> attributes = new ArrayList<Attribute>();
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
                        attr.setAttributeGroup(key);
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
                attr.setAttributeGroup(group);
                attributes.add(attr);
            }
        }
        return attributes;
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
            Map<String,List<Attribute>> byGroup = new HashMap<String,List<Attribute>>();
            for (Attribute attr : attributes) {
                List<Attribute> groupAttrs = byGroup.get(attr.getAttributeGroup());
                if (groupAttrs == null) {
                    groupAttrs = new ArrayList<Attribute>();
                    byGroup.put(attr.getAttributeGroup(), groupAttrs);
                }
                groupAttrs.add(attr);
            }
            for (String group : byGroup.keySet()) {
                if (group == null) {
                    for (Attribute ungroupedAttr : byGroup.get(group))
                        attrsJson.put(ungroupedAttr.getAttributeName(), ungroupedAttr.getAttributeValue());
                }
                else {
                    JSONObject groupJson = new JsonObject();
                    attrsJson.put(group, groupJson);
                    for (Attribute groupedAttr : byGroup.get(group))
                        groupJson.put(groupedAttr.getAttributeName(), groupedAttr.getAttributeValue());
                }
            }
        }
        else {
            for (Attribute attr : attributes) {
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

}
