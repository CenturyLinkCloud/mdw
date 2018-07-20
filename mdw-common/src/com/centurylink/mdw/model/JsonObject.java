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

import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONObject;

/**
 * Extends org.json.JSONObject to offer predictable ordering of object keys.
 * This is especially useful for comparing expected versus actual stringified JSON results
 * (for either automated or eyeball comparisons).
 */
public class JsonObject extends JSONObject {

    public JsonObject() {
    }

    public JsonObject(String source) {
        super(source);
    }

    public Set<String> keySet() {
        return new TreeSet<String>(super.keySet());
    }

    protected Set<Entry<String,Object>> entrySet() {
        Set<Entry<String,Object>> entries = new TreeSet<Entry<String,Object>>(new Comparator<Entry<String,Object>>() {
            public int compare(Entry<String,Object> e1, Entry<String,Object> e2) {
                return e1.getKey().compareTo(e2.getKey());
            }
        });
        entries.addAll(super.entrySet());
        return entries;
    }
}
