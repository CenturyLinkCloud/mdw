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

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.JsonTranslator;
import com.centurylink.mdw.translator.TranslationException;

public class JsonableTranslator extends DocumentReferenceTranslator implements JsonTranslator {

    public Object realToObject(String str) throws TranslationException {
        try {
            JSONObject json = new JSONObject(str);
            return createJsonable(json);
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    public String realToString(Object object) throws TranslationException {
        Jsonable jsonable = (Jsonable) object;
        JSONObject json = new JSONObject();
        try {
            json.put(JSONABLE_TYPE, jsonable.getClass().getName());
            json.put(jsonable.getJsonName(), jsonable.getJson());
            return json.toString(2);
        }
        catch (JSONException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    public JSONObject toJson(Object obj) throws TranslationException {
        try {
            return ((Jsonable)obj).getJson();
        }
        catch (JSONException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    public Object fromJson(JSONObject json) throws TranslationException {
        try {
            return createJsonable(json);
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }
}
