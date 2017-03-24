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
package com.centurylink.mdw.common.translator.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;

public class StringStringMapTranslator extends DocumentReferenceTranslator {

    @Override
    public Object realToObject(String json) throws TranslationException {
        try {
            Map<String,String> stringMap = new HashMap<String,String>();
            JSONObject jsonObject = new JSONObject(json);
            String[] stringNames = JSONObject.getNames(jsonObject);
            if (stringNames != null) {
                for (int i = 0; i < stringNames.length; i++) {
                    stringMap.put(stringNames[i], jsonObject.getString(stringNames[i]));
                }
            }
            return stringMap;
        }
        catch (JSONException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    public String realToString(Object object) throws TranslationException {
        Map<String,String> stringMap = (Map<String,String>)object;
        JSONObject jsonObject = new JSONObject();
        Iterator<String> it = stringMap.keySet().iterator();
        try {
            while (it.hasNext()) {
                String name = it.next();
                String val = stringMap.get(name);
                if (val == null) {
                    continue;
                }
                jsonObject.put(name, val);
            }
        return jsonObject.toString(2);
        }
        catch (JSONException e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }
}